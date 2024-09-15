package com.angorasix.gateway.infrastructure.filters;

import com.angorasix.commons.infrastructure.constants.AngoraSixInfrastructure;
import com.angorasix.gateway.infrastructure.config.api.GatewayApiConfigurations;
import com.angorasix.gateway.infrastructure.config.constants.ConfigConstants;
import com.angorasix.gateway.infrastructure.config.internalroutes.GatewayInternalRoutesConfigurations;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.rewrite.ModifyRequestBodyGatewayFilterFactory;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
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

  /* default */ final Logger logger = LoggerFactory.getLogger(
      ValidateIsAdminGatewayFilterFactory.class);

  private final transient ParameterizedTypeReference<Map<String, Object>> jsonType =
      new ParameterizedTypeReference<>() {
      };

  private final transient GatewayInternalRoutesConfigurations internalRoutesConfigs;

  private final transient GatewayApiConfigurations apiConfigs;

  private final transient ConfigConstants configConstants;

  private final transient ModifyRequestBodyGatewayFilterFactory modifyRequestBodyFilter;

  /**
   * Main constructor with required params.
   *
   * @param modifyRequestBodyFilter delegating Modify Request Body Filter
   * @param apiConfigs              API configs
   * @param internalRoutesConfigs   internal Routes configs to route composing call
   */
  public ValidateIsAdminGatewayFilterFactory(final GatewayApiConfigurations apiConfigs,
      final ModifyRequestBodyGatewayFilterFactory modifyRequestBodyFilter,
      final ConfigConstants configConstants,
      final GatewayInternalRoutesConfigurations internalRoutesConfigs) {
    super(Config.class);
    this.apiConfigs = apiConfigs;
    this.configConstants = configConstants;
    this.internalRoutesConfigs = internalRoutesConfigs;
    this.modifyRequestBodyFilter = modifyRequestBodyFilter;
  }

  @Override
  public GatewayFilter apply(final Config config) {
    return modifyRequestBodyFilter.apply((c) -> c.setRewriteFunction(Object.class, Object.class,
        (filterExchange, input) -> ReactiveSecurityContextHolder.getContext()
            .map(SecurityContext::getAuthentication)
            .map(JwtAuthenticationToken.class::cast)
            .flatMap(
                auth -> {
                  if (logger.isDebugEnabled()) {
                    logger.debug("Validating if is Admin...");
                    logger.debug(config.toString());
                  }
                  final String projectId = obtainProjectId(filterExchange, input,
                      config.getProjectIdBodyField());
                  final String resolvedAdminEndpoint = internalRoutesConfigs.projectsCore()
                      .isAdminEndpoint()
                      .replace(configConstants.projectIdPlaceholder(), projectId);
                  // Request to a path managed by the Gateway
                  final WebClient client = WebClient.create();
                  return client.get().uri(
                          UriComponentsBuilder.fromUriString(
                                  apiConfigs.projects().core().baseUrl())
                              .pathSegment(apiConfigs.projects().core().outBasePath(),
                                  resolvedAdminEndpoint).build().toUri())
                      .header(HttpHeaders.AUTHORIZATION,
                          "Bearer %s".formatted(auth.getToken().getTokenValue()))
                      .header(config.getGoogleCloudRunAuthHeader(),
                          "Bearer %s".formatted(
                              Optional.ofNullable(filterExchange.getAttribute("%s-%s".formatted(
                                      configConstants.googleTokenAttribute(),
                                      apiConfigs.projects().core().baseUrl())))
                                  .map(Object::toString)
                                  .orElse("")))
                      .exchangeToMono(response -> response.bodyToMono(jsonType))
                      .switchIfEmpty(Mono.just(Collections.emptyMap()))
                      .map(isAdminResponse -> {
                        processIsAdminResponse(config, filterExchange, isAdminResponse);
                        return Optional.ofNullable(input).orElse(Collections.emptyMap());
                      });
                }).switchIfEmpty(config.isAnonymousRequestAllowed() ? Mono.justOrEmpty(input)
                : Mono.error(new IllegalArgumentException(
                    "Validate Project Admin is not an optional step for this endpoint")))));
  }

  private void processIsAdminResponse(final Config config, final ServerWebExchange filterExchange,
      final Map<String, Object> isAdminResponse) {
    final boolean isAdmin =
        isAdminResponse.containsKey(
            internalRoutesConfigs.projectsCoreParams()
                .isAdminResponseField())
            && isAdminResponse.get(
            internalRoutesConfigs.projectsCoreParams()
                .isAdminResponseField()).equals(true);
    if (logger.isDebugEnabled()) {
      logger.debug("isAdmin result: %s".formatted(isAdmin));
    }
    if (!config.isNonAdminRequestAllowed() && !isAdmin) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN,
          "Only Project admin can proceed");
    }
    filterExchange.getAttributes()
        .put(configConstants.isAssociatedEntityAdminAttribute(),
            isAdmin);
  }

  private String obtainProjectId(final ServerWebExchange exchange, final Object input,
      final String projectIdBodyField) {
    return Optional.ofNullable(
            exchange.getAttribute(ServerWebExchangeUtils.URI_TEMPLATE_VARIABLES_ATTRIBUTE))
        .map(Map.class::cast).map(attributes -> attributes.get(configConstants.projectIdParam()))
        .map(String.class::cast)
        .orElseGet(() -> obtainProjectIdFromInputBody(input, projectIdBodyField));
  }

  private String obtainProjectIdFromInputBody(final Object input, final String projectIdBodyField) {
    final boolean isMap = Map.class.isAssignableFrom(input.getClass());
    if (!isMap) {
      throw new IllegalArgumentException(
          String.format("Trying to validate adminId from input of type [%s]",
              input.getClass().getName()));
    }
    final Map<String, Object> requestBody = (Map<String, Object>) input;
    return (String) requestBody.get(projectIdBodyField);
  }


  @Override
  public List<String> shortcutFieldOrder() {
    return Arrays.asList("nonAdminRequestAllowed", "anonymousRequestAllowed",
        "projectIdBodyField", "googleCloudRunAuthHeader");
  }

  /**
   * <p>
   * Config class to use for AbstractGatewayFilterFactory.
   * </p>
   */
  public static class Config {

    private String projectIdBodyField = "";
    private boolean nonAdminRequestAllowed;
    private boolean anonymousRequestAllowed;

    private String googleCloudRunAuthHeader =
        AngoraSixInfrastructure.GOOGLE_CLOUD_RUN_INFRA_AUTH_HEADER;

    public String getProjectIdBodyField() {
      return projectIdBodyField;
    }

    public void setProjectIdBodyField(final String projectIdBodyField) {
      this.projectIdBodyField = projectIdBodyField;
    }

    public boolean isNonAdminRequestAllowed() {
      return nonAdminRequestAllowed;
    }

    public void setNonAdminRequestAllowed(final boolean nonAdminRequestAllowed) {
      this.nonAdminRequestAllowed = nonAdminRequestAllowed;
    }

    public boolean isAnonymousRequestAllowed() {
      return anonymousRequestAllowed;
    }

    public void setAnonymousRequestAllowed(final boolean anonymousRequestAllowed) {
      this.anonymousRequestAllowed = anonymousRequestAllowed;
    }

    public String getGoogleCloudRunAuthHeader() {
      return googleCloudRunAuthHeader;
    }

    public void setGoogleCloudRunAuthHeader(final String googleCloudRunAuthHeader) {
      this.googleCloudRunAuthHeader = googleCloudRunAuthHeader;
    }
  }
}