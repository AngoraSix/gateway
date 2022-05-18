package com.angorasix.gateway.infrastructure.filters;

import com.angorasix.gateway.infrastructure.config.api.GatewayApiConfigurations;
import com.angorasix.gateway.infrastructure.config.internalroutes.GatewayInternalRoutesConfigurations;
import com.angorasix.gateway.infrastructure.models.headers.A6ContributorHeader;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.rewrite.ModifyRequestBodyGatewayFilterFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Filter to validate the user is Admin of the manipulated Resource.
 * </p>
 *
 * @author rozagerardo
 */
@Component
public class ValidateIsAdminGatewayFilterFactory extends
    AbstractGatewayFilterFactory<ValidateIsAdminGatewayFilterFactory.Config> {

  private static final String PROJECT_PRESENTATION_ID_PARAM_PLACEHOLDER = ":projectId";

  private ObjectMapper objectMapper;

  ParameterizedTypeReference<Map<String, Object>> jsonType =
      new ParameterizedTypeReference<Map<String, Object>>() {
      };

  GatewayInternalRoutesConfigurations internalRoutesConfigs;

  GatewayApiConfigurations apiConfigs;

  ModifyRequestBodyGatewayFilterFactory modifyRequestBodyFilter;

  public ValidateIsAdminGatewayFilterFactory(ObjectMapper objectMapper,
      ModifyRequestBodyGatewayFilterFactory modifyRequestBodyFilter,
      GatewayApiConfigurations apiConfigs,
      GatewayInternalRoutesConfigurations internalRoutesConfigs) {
    super(Config.class);
    this.objectMapper = objectMapper;
    this.apiConfigs = apiConfigs;
    this.internalRoutesConfigs = internalRoutesConfigs;
    this.modifyRequestBodyFilter = modifyRequestBodyFilter;
  }

  @Override
  public GatewayFilter apply(final Config config) {
    return modifyRequestBodyFilter.apply((c) ->
        c.setRewriteFunction(Object.class, Object.class, (filterExchange, input) -> {
          if (!filterExchange.getResponse().getStatusCode().is2xxSuccessful() || input == null) {
            return Mono.justOrEmpty(input);
          }
          boolean isMap = Map.class.isAssignableFrom(input.getClass());
          if (!isMap) {
            throw new IllegalArgumentException(
                String.format("Trying to validate adminId from input of type [%s]",
                    input.getClass().getName()));
          }
          Map<String, Object> requestBody = (Map<String, Object>) input;

          return ReactiveSecurityContextHolder.getContext().map(
              SecurityContext::getAuthentication).flatMap(auth -> {
            String jsonContributor;
            A6ContributorHeader a6Contributor =
                (auth instanceof JwtAuthenticationToken) ? new A6ContributorHeader(
                    auth.getName(),
                    ((JwtAuthenticationToken) auth).getToken().getClaim("attributes"))
                    : new A6ContributorHeader(auth.getName(),
                        Collections.emptyMap());
            try {
              jsonContributor = objectMapper.writeValueAsString(a6Contributor);
            } catch (JsonProcessingException e) {
              e.printStackTrace();
              throw new RuntimeException("Error converting Principal to JSON.");
            }
            String encodedContributor = Base64.getUrlEncoder()
                .encodeToString(jsonContributor.getBytes());
            String projectId = (String) requestBody.get(config.getProjectIdBodyField());
            String resolvedAdminEndpoint = internalRoutesConfigs.getProjectsCore()
                .getIsAdminEndpoint()
                .replace(PROJECT_PRESENTATION_ID_PARAM_PLACEHOLDER, projectId);
            // Request to a path managed by the Gateway
            WebClient client = WebClient.create();
            return client.get()
                .uri(UriComponentsBuilder.fromUriString(apiConfigs.getProjects().getCoreBaseUrl())
                    .pathSegment(apiConfigs.getProjects().getCoreOutBasePath(),
                        resolvedAdminEndpoint)
                    .build().toUri())
                .header(apiConfigs.getCommon().getContributorHeader(),
                    encodedContributor)
                .exchangeToMono(response -> response.bodyToMono(jsonType)).map(isAdminResponse ->
                {
                  if (!isAdminResponse.containsKey("isAdmin") || isAdminResponse.get("isAdmin")
                      .equals(false)) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "You don't have permission to update entity");
                  }
                  return requestBody;
                });
          });
        }));
  }

  @Override
  public List<String> shortcutFieldOrder() {
    return Arrays.asList("projectIdBodyField");
  }

  /**
   * <p>
   * Config class to use for AbstractGatewayFilterFactory.
   * </p>
   */
  public static class Config {

    private String projectIdBodyField;

    public Config() {
    }

    public String getProjectIdBodyField() {
      return projectIdBodyField;
    }

    public void setProjectIdBodyField(String projectIdBodyField) {
      this.projectIdBodyField = projectIdBodyField;
    }
  }
}