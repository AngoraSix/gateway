package com.angorasix.gateway.infrastructure.filters;

import com.angorasix.commons.infrastructure.constants.AngoraSixInfrastructure;
import com.angorasix.gateway.infrastructure.config.api.GatewayApiConfigurations;
import com.angorasix.gateway.infrastructure.config.constants.ConfigConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;

/**
 * <p>
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
   * @param objectMapper      the ObjectMapper configured in the service
   * @param apiConfigurations API configurations to make requests to the Contributor service
   */
  public AddIsAdminHeaderGatewayFilterFactory(final ObjectMapper objectMapper,
      final GatewayApiConfigurations apiConfigurations, final ConfigConstants configConstants) {
    super(Config.class);
    this.configConstants = configConstants;
  }

  @Override
  public GatewayFilter apply(final Config config) {
    return (exchange, chain) -> chain.filter(
        exchange.getAttributes()
            .containsKey(
                configConstants.isProjectAdminAttribute()) ?
            exchange.mutate()
                .request(req -> req.header(AngoraSixInfrastructure.REQUEST_IS_ADMIN_HINT_HEADER,
                    exchange.getAttribute(configConstants.isProjectAdminAttribute()).toString()))
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
