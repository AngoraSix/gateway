package com.angorasix.gateway.infrastructure.filters;

import com.angorasix.commons.infrastructure.constants.AngoraSixInfrastructure;
import com.angorasix.commons.presentation.dto.IsAdminDto;
import com.angorasix.gateway.infrastructure.config.api.GatewayApiConfigurations;
import com.angorasix.gateway.infrastructure.config.constants.ConfigConstants;
import com.angorasix.gateway.infrastructure.config.internalroutes.GatewayInternalRoutesConfigurations;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.rewrite.ModifyRequestBodyGatewayFilterFactory;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
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
 * <p>Filter to validate the user is Admin of the manipulated Resource, for GET requests (avoiding
 * messing up the request body, which can generate issues depending on the underlying
 * infrastructure).
 * </p>
 *
 * @author rozagerardo
 */
@Component
public class ValidateIsAdminGatewayFilterFactory extends
    AbstractGatewayFilterFactory<ValidateIsAdminGatewayFilterFactory.Config> {

  private static final String BEARER_TOKEN_PLACEHOLDER = "Bearer %s";

  /* default */ final Logger logger = LoggerFactory.getLogger(
      ValidateIsAdminGatewayFilterFactory.class);

  private final transient GatewayInternalRoutesConfigurations internalRoutesConfigs;

  private final transient GatewayApiConfigurations apiConfigs;

  private final transient ConfigConstants configConstants;

  private final transient ModifyRequestBodyGatewayFilterFactory modifyRequestBodyFilter;

  /**
   * Main constructor with required params.
   *
   * @param apiConfigs            API configs
   * @param internalRoutesConfigs internal Routes configs to route composing call
   */
  public ValidateIsAdminGatewayFilterFactory(
      final GatewayApiConfigurations apiConfigs,
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
    return config.isForGetRequest()
        ? (filterExchange, chain) -> checkAdminWithoutTemperingBody(config, filterExchange, chain)
        : checkAdminAccessingBody(config);
  }

  private GatewayFilter checkAdminAccessingBody(final Config config) {
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
                  final String associatedEntityId = obtainAssociatedEntityId(config,
                      filterExchange, input, config.projectIdBodyField);
                  final String serviceBaseUrl = obtainServiceBaseUrl(config, apiConfigs);
                  final String serviceOutBaseUrl = obtainServiceOutBaseUrl(config,
                      apiConfigs);
                  final String resolvedAdminEndpoint = obtainAdminEndpoint(config,
                      associatedEntityId);
                  // Request to a path managed by the Gateway
                  final WebClient client = WebClient.create();
                  return client.get().uri(
                          UriComponentsBuilder.fromUriString(serviceBaseUrl)
                              .pathSegment(serviceOutBaseUrl, resolvedAdminEndpoint)
                              .build().toUri())
                      .header(HttpHeaders.AUTHORIZATION,
                          BEARER_TOKEN_PLACEHOLDER.formatted(
                              auth.getToken().getTokenValue()))
                      .header(config.getGoogleCloudRunAuthHeader(),
                          BEARER_TOKEN_PLACEHOLDER.formatted(
                              Optional.ofNullable(
                                      filterExchange.getAttribute("%s-%s".formatted(
                                          configConstants.googleTokenAttribute(),
                                          apiConfigs.projects().core().baseUrl())))
                                  .map(Object::toString)
                                  .orElse("")))
                      .exchangeToMono(response -> response.bodyToMono(IsAdminDto.class))
                      .switchIfEmpty(Mono.just(new IsAdminDto(false)))
                      .map(isAdminResponse -> {
                        processIsAdminResponse(config, filterExchange, isAdminResponse);
                        return Optional.ofNullable(input).orElse(Collections.emptyMap());
                      });
                })
            .switchIfEmpty(config.isAnonymousRequestAllowed() ? Mono.justOrEmpty(input)
                : Mono.error(new IllegalArgumentException(
                    "Validate Project Admin is not an optional step for this endpoint")))));
  }

  private @NotNull Mono<Void> checkAdminWithoutTemperingBody(final Config config,
      final ServerWebExchange filterExchange,
      final GatewayFilterChain chain) {

    return ReactiveSecurityContextHolder.getContext()
        .map(SecurityContext::getAuthentication)
        .map(JwtAuthenticationToken.class::cast)
        .flatMap(
            auth -> {
              if (logger.isDebugEnabled()) {
                logger.debug("Validating if is Admin (without modifying body)...");
                logger.debug(config.toString());
              }
              final String associatedEntityId = obtainAssociatedEntityId(config, filterExchange,
                  null, null);
              final String serviceBaseUrl = obtainServiceBaseUrl(config, apiConfigs);
              final String serviceOutBaseUrl = obtainServiceOutBaseUrl(config, apiConfigs);
              final String resolvedAdminEndpoint = obtainAdminEndpoint(config, associatedEntityId);
              // Request to a path managed by the Gateway
              final WebClient client = WebClient.create();
              return client.get().uri(
                      UriComponentsBuilder.fromUriString(serviceBaseUrl)
                          .pathSegment(serviceOutBaseUrl, resolvedAdminEndpoint)
                          .build().toUri())
                  .header(HttpHeaders.AUTHORIZATION,
                      BEARER_TOKEN_PLACEHOLDER.formatted(auth.getToken().getTokenValue()))
                  .header(config.getGoogleCloudRunAuthHeader(),
                      BEARER_TOKEN_PLACEHOLDER.formatted(
                          Optional.ofNullable(filterExchange.getAttribute("%s-%s".formatted(
                                  configConstants.googleTokenAttribute(),
                                  apiConfigs.projects().core().baseUrl())))
                              .map(Object::toString)
                              .orElse("")))
                  .exchangeToMono(response -> response.bodyToMono(IsAdminDto.class))
                  .switchIfEmpty(Mono.just(new IsAdminDto(false)))
                  .map(isAdminResponse -> {
                    processIsAdminResponse(config, filterExchange, isAdminResponse);
                    return filterExchange;
                  });
            }).flatMap(chain::filter);
  }

  private String obtainAssociatedEntityId(final Config config, final ServerWebExchange exchange,
      final Object input, final String projectIdBodyField) {
    final String associatedEntityIdParam =
        config.isForProjectManagement() ? configConstants.projectManagementIdParam()
            : configConstants.projectIdParam();
    return Optional.ofNullable(
            exchange.getAttribute(ServerWebExchangeUtils.URI_TEMPLATE_VARIABLES_ATTRIBUTE))
        .map(Map.class::cast).map(attributes -> attributes.get(associatedEntityIdParam))
        .map(String.class::cast)
        .orElseGet(() -> obtainProjectIdFromInputBody(config, input, projectIdBodyField));
  }

  private String obtainProjectIdFromInputBody(final Config config, final Object input,
      final String projectIdBodyField) {
    final boolean isMap = input != null && Map.class.isAssignableFrom(input.getClass());
    if (!isMap && logger.isDebugEnabled()) {
      logger.debug("Trying to validate adminId from input of type [%s]",
          input.getClass().getName());
      return null;
    }
    final Map<String, Object> requestBody = (Map<String, Object>) input;
    final String associatedEntityId = (String) requestBody.get(projectIdBodyField);
    if (associatedEntityId == null) {
      throw new IllegalArgumentException(
          "Can't obtain %s from request URI".formatted(
              config.isForProjectManagement() ? configConstants.projectManagementIdParam()
                  : configConstants.projectIdParam()));
    }
    return associatedEntityId;
  }

  private String obtainServiceBaseUrl(final Config config,
      final GatewayApiConfigurations apiConfigs) {
    return config.isForProjectManagement() ? apiConfigs.managements().core().baseUrl()
        : apiConfigs.projects().core().baseUrl();
  }

  private String obtainServiceOutBaseUrl(final Config config,
      final GatewayApiConfigurations apiConfigs) {
    return config.isForProjectManagement() ? apiConfigs.managements().core().outBasePath()
        : apiConfigs.projects().core().outBasePath();
  }

  private String obtainAdminEndpoint(final Config config, final String associatedEntityId) {
    final String associatedEntityIdParamPlaceholder =
        config.isForProjectManagement() ? configConstants.projectManagementIdPlaceholder()
            : configConstants.projectIdPlaceholder();
    return config.isForProjectManagement() ? internalRoutesConfigs.managementsCore()
        .isAdminEndpoint()
        .replace(associatedEntityIdParamPlaceholder, associatedEntityId)
        : internalRoutesConfigs.projectsCore()
            .isAdminEndpoint()
            .replace(associatedEntityIdParamPlaceholder, associatedEntityId);
  }

  private void processIsAdminResponse(final Config config, final ServerWebExchange filterExchange,
      final IsAdminDto isAdminResponse) {
    if (!config.isNonAdminRequestAllowed() && !isAdminResponse.isAdmin()) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN,
          "Only admin can proceed");
    }
    if (logger.isDebugEnabled()) {
      logger.debug("isAdmin result: %s".formatted(isAdminResponse.isAdmin()));
    }
    filterExchange.getAttributes()
        .put(configConstants.isAdminAttribute(),
            isAdminResponse.isAdmin());
  }

  @Override
  public List<String> shortcutFieldOrder() {
    return Arrays.asList("projectIdBodyField", "nonAdminRequestAllowed", "forProjectManagement",
        "anonymousRequestAllowed",
        "googleCloudRunAuthHeader");
  }

  /**
   * <p>
   * Config class to use for AbstractGatewayFilterFactory.
   * </p>
   */
  public static class Config {

    private boolean forGetRequest;
    private String projectIdBodyField = "";
    private boolean nonAdminRequestAllowed;
    private boolean forProjectManagement;
    private boolean anonymousRequestAllowed;

    private String googleCloudRunAuthHeader =
        AngoraSixInfrastructure.GOOGLE_CLOUD_RUN_INFRA_AUTH_HEADER;

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

    public boolean isForProjectManagement() {
      return forProjectManagement;
    }

    public void setForProjectManagement(final String associatedEntity) {
      this.forProjectManagement = "projectManagement".equals(associatedEntity);
    }

    public boolean isForGetRequest() {
      return forGetRequest;
    }

    public String getProjectIdBodyField() {
      return projectIdBodyField;
    }

    /**
     * Set the projectIdBodyField, if it's a GET request, it will be ignored.
     *
     * @param projectIdBodyField the field to obtain the projectId from the request body
     */
    public void setProjectIdBodyField(final String projectIdBodyField) {
      if ("GET".equals(projectIdBodyField)) {
        this.forGetRequest = true;
      } else {
        this.projectIdBodyField = projectIdBodyField;
      }
    }
  }
}