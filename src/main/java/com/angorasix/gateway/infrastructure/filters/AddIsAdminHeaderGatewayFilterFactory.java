package com.angorasix.gateway.infrastructure.filters;

import com.angorasix.commons.infrastructure.constants.AngoraSixInfrastructure;
import com.angorasix.gateway.infrastructure.config.constants.ConfigConstants;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;

/**
 * <p>
 * Filter to add Admin Header from attributes.
 * </p>
 *
 * @author rozagerardo
 */
@Component
public class AddIsAdminHeaderGatewayFilterFactory extends
    AbstractGatewayFilterFactory<AddIsAdminHeaderGatewayFilterFactory.Config> {

  private final transient ConfigConstants configConstants;

  /**
   * Constructor with required params.
   *
   * @param configConstants all the required constants for the Gateway service
   */
  public AddIsAdminHeaderGatewayFilterFactory(final ConfigConstants configConstants) {
    super(Config.class);
    this.configConstants = configConstants;
  }

  @Override
  public GatewayFilter apply(final Config config) {
    return (exchange, chain) -> chain.filter(
        exchange.getAttributes()
            .containsKey(
                configConstants.isAdminAttribute())
            ? exchange.mutate()
            .request(req -> req.header(AngoraSixInfrastructure.REQUEST_IS_ADMIN_HINT_HEADER,
                exchange.getAttribute(configConstants.isAdminAttribute()).toString()))
            .build() : exchange);
  }

  /**
   * <p>
   * Config class to use for AbstractGatewayFilterFactory.
   * </p>
   */
  public static class Config {

  }
}
