package com.angorasix.gateway.infrastructure.filters;

import com.angorasix.gateway.infrastructure.config.api.GatewayApiConfigurations;
import com.angorasix.gateway.infrastructure.config.constants.ConfigConstants;
import com.angorasix.gateway.infrastructure.config.internalroutes.GatewayInternalRoutesConfigurations;
import com.angorasix.gateway.infrastructure.models.headers.A6ContributorHeaderHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
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

  private final transient ObjectMapper objectMapper;

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
    this.objectMapper = objectMapper;
  }

  @Override
  public GatewayFilter apply(final Config config) {
    return (filterExchange, chain) -> ReactiveSecurityContextHolder.getContext()
        .map(SecurityContext::getAuthentication)
        .map(A6ContributorHeaderHelper::buildFromAuthentication)
        .flatMap(a6Contributor -> {
          final String projectsEndpoint = internalRoutesConfigs.projectsCore()
              .projectsEndpoint();
          // Request to a path managed by the Gateway
          final WebClient client = WebClient.create();
          final String encodedContributor = A6ContributorHeaderHelper.encodeContributorHeader(
              a6Contributor, objectMapper);
          final String adminId = Optional.ofNullable(
                  filterExchange.getRequest().getQueryParams()
                      .getFirst(config.getAdminIdQueryParamKey()))
              .orElse(a6Contributor.getContributorId());
          return client.get().uri(
                  UriComponentsBuilder.fromUriString(
                          apiConfigs.projects().core().baseUrl())
                      .pathSegment(apiConfigs.projects().core().outBasePath(),
                          projectsEndpoint)
                      .queryParam(
                          internalRoutesConfigs.projectsCoreParams().adminIdQueryParam(),
                          adminId)
                      .build().toUri())
              // TODO: Fix this, now using Token Relay
//              .header(apiConfigs.common().contributorHeader(), encodedContributor)
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
                        adminId.equals(a6Contributor.getContributorId()));
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