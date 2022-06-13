package com.angorasix.gateway.infrastructure.filters;

import com.angorasix.gateway.infrastructure.config.api.GatewayApiConfigurations;
import com.angorasix.gateway.infrastructure.config.infrastructure.InfrastructureConfigurations;
import com.angorasix.gateway.infrastructure.config.internalroutes.GatewayInternalRoutesConfigurations;
import com.angorasix.gateway.infrastructure.models.headers.A6ContributorHeaderHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.rewrite.ModifyRequestBodyGatewayFilterFactory;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
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

  private static final String PROJECT_PRESENTATION_ID_PARAM = "projectId";

  private static final String PROJECT_PRESENTATION_ID_PARAM_PLACEHOLDER =
      ":" + PROJECT_PRESENTATION_ID_PARAM;

  private static final String IS_ADMIN_RESPONSE_FIELD = "isAdmin";

  private ObjectMapper objectMapper;

  ParameterizedTypeReference<Map<String, Object>> jsonType =
      new ParameterizedTypeReference<Map<String, Object>>() {
      };

  private GatewayInternalRoutesConfigurations internalRoutesConfigs;

  private InfrastructureConfigurations infrastructureConfigs;

  private GatewayApiConfigurations apiConfigs;

  private ModifyRequestBodyGatewayFilterFactory modifyRequestBodyFilter;

  public ValidateIsAdminGatewayFilterFactory(ObjectMapper objectMapper,
      ModifyRequestBodyGatewayFilterFactory modifyRequestBodyFilter,
      GatewayApiConfigurations apiConfigs,
      GatewayInternalRoutesConfigurations internalRoutesConfigs,
      InfrastructureConfigurations infrastructureConfigs) {
    super(Config.class);
    this.objectMapper = objectMapper;
    this.apiConfigs = apiConfigs;
    this.internalRoutesConfigs = internalRoutesConfigs;
    this.modifyRequestBodyFilter = modifyRequestBodyFilter;
    this.infrastructureConfigs = infrastructureConfigs;
  }

  @Override
  public GatewayFilter apply(final Config config) {
    return modifyRequestBodyFilter.apply((c) ->
        c.setRewriteFunction(Object.class, Object.class,
            (filterExchange, input) -> ReactiveSecurityContextHolder.getContext().map(
                SecurityContext::getAuthentication).map(
                auth -> A6ContributorHeaderHelper.buildAndEncodeFromAuthentication(auth,
                    objectMapper)).flatMap(encodedA6Contributor -> {
              String projectId = obtainProjectId(filterExchange, input,
                  config.getProjectIdBodyField());
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
                      encodedA6Contributor)
                  .exchangeToMono(response -> response.bodyToMono(jsonType))
                  .switchIfEmpty(Mono.just(Collections.emptyMap())).map(isAdminResponse ->
                  {
                    boolean isAdmin =
                        isAdminResponse.containsKey(IS_ADMIN_RESPONSE_FIELD) && isAdminResponse.get(
                                IS_ADMIN_RESPONSE_FIELD)
                            .equals(true);
                    if (!config.isNonAdminRequestAllowed() && !isAdmin) {
                      throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                          "Only Project admin can proceed");
                    }
                    filterExchange.getAttributes()
                        .put(this.infrastructureConfigs.getExchangeAttributes().getIsProjectAdmin(),
                            isAdmin);
                    return Optional.ofNullable(input).orElse(Collections.emptyMap());
                  });
            }).switchIfEmpty(
                config.isAnonymousRequestAllowed() ? Mono.justOrEmpty(input)
                    : Mono.error(new IllegalArgumentException(
                        "Validate Project Admin is not an optional step for this endpoint")))));
  }

  private String obtainProjectId(ServerWebExchange exchange, Object input,
      String projectIdBodyField) {

    return Optional.ofNullable(
            exchange.getAttribute(ServerWebExchangeUtils.URI_TEMPLATE_VARIABLES_ATTRIBUTE))
        .map(Map.class::cast).map(attributes -> attributes.get(PROJECT_PRESENTATION_ID_PARAM))
        .map(String.class::cast)
        .orElseGet(() -> obtainProjectIdFromInputBody(input, projectIdBodyField));
  }

  private String obtainProjectIdFromInputBody(Object input, String projectIdBodyField) {
    boolean isMap = Map.class.isAssignableFrom(input.getClass());
    if (!isMap) {
      throw new IllegalArgumentException(
          String.format("Trying to validate adminId from input of type [%s]",
              input.getClass().getName()));
    }
    Map<String, Object> requestBody = (Map<String, Object>) input;
    return (String) requestBody.get(projectIdBodyField);
  }


  @Override
  public List<String> shortcutFieldOrder() {
    return Arrays.asList("anonymousRequestAllowed", "nonAdminRequestAllowed", "projectIdBodyField");
  }

  /**
   * <p>
   * Config class to use for AbstractGatewayFilterFactory.
   * </p>
   */
  public static class Config {

    private String projectIdBodyField = "";
    private boolean nonAdminRequestAllowed = false;
    private boolean anonymousRequestAllowed = false;

    public Config() {
    }

    public String getProjectIdBodyField() {
      return projectIdBodyField;
    }

    public void setProjectIdBodyField(String projectIdBodyField) {
      this.projectIdBodyField = projectIdBodyField;
    }

    public boolean isNonAdminRequestAllowed() {
      return nonAdminRequestAllowed;
    }

    public void setNonAdminRequestAllowed(boolean nonAdminRequestAllowed) {
      this.nonAdminRequestAllowed = nonAdminRequestAllowed;
    }

    public boolean isAnonymousRequestAllowed() {
      return anonymousRequestAllowed;
    }

    public void setAnonymousRequestAllowed(boolean anonymousRequestAllowed) {
      this.anonymousRequestAllowed = anonymousRequestAllowed;
    }
  }
}