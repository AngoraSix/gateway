package com.angorasix.gateway.infrastructure.filters.contributors;

import com.angorasix.gateway.infrastructure.config.api.GatewayApiConfigurations;
import com.angorasix.gateway.infrastructure.config.infrastructure.InfrastructureConfigurations;
import com.angorasix.gateway.infrastructure.models.headers.A6ContributorHeaderHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Filter to add Contributor Information in header.
 * </p>
 *
 * @author rozagerardo
 */
@Component
public class AddContributorHeaderGatewayFilterFactory extends
    AbstractGatewayFilterFactory<AddContributorHeaderGatewayFilterFactory.Config> {

  private String contributorHeader;

  private ObjectMapper objectMapper;

  private InfrastructureConfigurations infrastructureConfigs;

  public AddContributorHeaderGatewayFilterFactory(ObjectMapper objectMapper,
      GatewayApiConfigurations apiConfigurations,
      InfrastructureConfigurations infrastructureConfigs) {
    super(Config.class);
    this.contributorHeader = apiConfigurations.getCommon().getContributorHeader();
    this.objectMapper = objectMapper;
    this.infrastructureConfigs = infrastructureConfigs;
  }

  @Override
  public GatewayFilter apply(final Config config) {
    return (exchange, chain) -> ReactiveSecurityContextHolder.getContext().map(
            SecurityContext::getAuthentication).map(A6ContributorHeaderHelper::buildFromAuthentication)
        .map(a6Contributor -> {
          if (exchange.getAttributes()
              .containsKey(
                  this.infrastructureConfigs.getExchangeAttributes().getIsProjectAdmin())) {
            a6Contributor.setProjectAdmin(exchange.getAttribute(
                this.infrastructureConfigs.getExchangeAttributes().getIsProjectAdmin()));
          }
          String encodedContributor = A6ContributorHeaderHelper.encodeContributorHeader(
              a6Contributor, objectMapper);
          return exchange.mutate().request(req -> req.header(contributorHeader, encodedContributor))
              .build();
        }).switchIfEmpty(Mono.just(exchange))
        .flatMap(chain::filter);

  }

  public static class Config {

    public Config() {
    }
  }
}