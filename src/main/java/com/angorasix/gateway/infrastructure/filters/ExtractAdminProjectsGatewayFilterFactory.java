package com.angorasix.gateway.infrastructure.filters;

import com.angorasix.gateway.infrastructure.config.api.GatewayApiConfigurations;
import com.angorasix.gateway.infrastructure.config.constants.ConfigConstants;
import com.angorasix.gateway.infrastructure.config.internalroutes.GatewayInternalRoutesConfigurations;
import com.angorasix.gateway.infrastructure.models.headers.A6ContributorHeaderHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Filter to request Administered Projects and add them as a exchange attribute for next filters.
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
          final String projectsEndpoint = internalRoutesConfigs.getProjectsCore()
              .getProjectsEndpoint();
          // Request to a path managed by the Gateway
          final WebClient client = WebClient.create();
          final String encodedContributor = A6ContributorHeaderHelper.encodeContributorHeader(
              a6Contributor, objectMapper);
          return client.get().uri(
                  UriComponentsBuilder.fromUriString(
                          apiConfigs.getProjects().getCoreBaseUrl())
                      .pathSegment(apiConfigs.getProjects().getCoreOutBasePath(),
                          projectsEndpoint)
                      .queryParam(
                          internalRoutesConfigs.getProjectsCoreParams().getAdminIdQueryParam(),
                          a6Contributor.getContributorId())
                      .build().toUri())
              .header(apiConfigs.getCommon().getContributorHeader(), encodedContributor)
              .exchangeToFlux(response -> response.bodyToFlux(jsonType))
              .switchIfEmpty(Mono.just(Collections.emptyMap())).map(e -> (String) e.get(
                  internalRoutesConfigs.getProjectsCoreParams()
                      .getProjectIdResponseField())).collectList()
              .map(projectIds -> {
                filterExchange.getAttributes()
                    .put(configConstants.getProjectIdsAttribute(),
                        projectIds);
                filterExchange.getAttributes()
                    .put(configConstants.getIsProjectAdminAttribute(),
                        true);
                return filterExchange;
              });
        }).flatMap(chain::filter);
  }

  @Override
  public List<String> shortcutFieldOrder() {
    return Arrays.asList("attributeField");
  }

  /**
   * <p>
   * Config class to use for AbstractGatewayFilterFactory.
   * </p>
   */
  public static class Config {

  }
}