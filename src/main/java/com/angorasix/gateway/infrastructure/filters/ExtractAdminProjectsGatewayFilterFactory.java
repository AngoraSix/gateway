package com.angorasix.gateway.infrastructure.filters;

import com.angorasix.commons.infrastructure.oauth2.constants.A6WellKnownClaims;
import com.angorasix.gateway.infrastructure.config.api.GatewayApiConfigurations;
import com.angorasix.gateway.infrastructure.config.constants.ConfigConstants;
import com.angorasix.gateway.infrastructure.config.internalroutes.GatewayInternalRoutesConfigurations;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * <p>
 * Filter to request Administered Projects for requested Contributor (or current Contributor
 * otherwise) and add them as a exchange attribute for next filters.
 * </p>
 *
 * @author rozagerardo
 */
@Component
public class ExtractAdminProjectsGatewayFilterFactory extends
    AbstractGatewayFilterFactory<ExtractAdminProjectsGatewayFilterFactory.Config> {

  private final transient ParameterizedTypeReference<Map<String, Object>> jsonType =
      new ParameterizedTypeReference<>() {
      };

  private final transient GatewayInternalRoutesConfigurations internalRoutesConfigs;

  private final transient GatewayApiConfigurations apiConfigs;

  private final transient ConfigConstants configConstants;

  /**
   * Main constructor with required params.
   *
   * @param apiConfigs            API configs
   * @param internalRoutesConfigs internal Routes configs to route composing call
   */
  public ExtractAdminProjectsGatewayFilterFactory(final GatewayApiConfigurations apiConfigs,
      final GatewayInternalRoutesConfigurations internalRoutesConfigs,
      final ConfigConstants configConstants,
      final ObjectMapper objectMapper) {
    super(Config.class);
    this.apiConfigs = apiConfigs;
    this.internalRoutesConfigs = internalRoutesConfigs;
    this.configConstants = configConstants;
  }

  @Override
  public GatewayFilter apply(final Config config) {
    return (filterExchange, chain) -> ReactiveSecurityContextHolder.getContext()
        .map(SecurityContext::getAuthentication)
        .map(JwtAuthenticationToken.class::cast)
        .flatMap(auth -> {
          final String projectsEndpoint = internalRoutesConfigs.projectsCore()
              .projectsEndpoint();
          // Request to a path managed by the Gateway
          final WebClient client = WebClient.create();
          final String authContributorId = auth.getToken()
              .getClaim(A6WellKnownClaims.CONTRIBUTOR_ID);
          final String adminId = Optional.ofNullable(
                  filterExchange.getRequest().getQueryParams()
                      .getFirst(config.getAdminIdQueryParamKey()))
              .orElse(authContributorId);
          return client.get().uri(
                  UriComponentsBuilder.fromUriString(
                          apiConfigs.projects().core().baseUrl())
                      .pathSegment(apiConfigs.projects().core().outBasePath(),
                          projectsEndpoint)
                      .queryParam(
                          internalRoutesConfigs.projectsCoreParams().adminIdQueryParam(),
                          adminId)
                      .build().toUri())
              .header(HttpHeaders.AUTHORIZATION,
                  "Bearer " + auth.getToken().getTokenValue())
              .exchangeToFlux(response -> response.bodyToFlux(jsonType))
              .map(e -> (String) e.get(
                  internalRoutesConfigs.projectsCoreParams()
                      .projectIdResponseField())).collectList()
              .map(projectIds -> {
                filterExchange.getAttributes()
                    .put(configConstants.projectIdsAttribute(),
                        projectIds);
                filterExchange.getAttributes()
                    .put(configConstants.isProjectAdminAttribute(),
                        adminId.equals(authContributorId));
                return filterExchange;
              });
        }).flatMap(chain::filter);
  }

  @Override
  public List<String> shortcutFieldOrder() {
    return Arrays.asList();
  }

  /**
   * <p>
   * Config class to use for AbstractGatewayFilterFactory.
   * </p>
   */
  public static class Config {

    private transient String adminIdQueryParamKey = "adminId";

    public String getAdminIdQueryParamKey() {
      return adminIdQueryParamKey;
    }

    public void setAdminIdQueryParamKey(final String adminIdQueryParamKey) {
      this.adminIdQueryParamKey = adminIdQueryParamKey;
    }
  }
}