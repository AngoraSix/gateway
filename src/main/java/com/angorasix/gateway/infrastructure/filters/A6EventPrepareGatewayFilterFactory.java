package com.angorasix.gateway.infrastructure.filters;

import com.angorasix.commons.infrastructure.constants.AngoraSixInfrastructure;
import com.angorasix.gateway.infrastructure.filters.A6EventPrepareGatewayFilterFactory.Config;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.CacheRequestBodyGatewayFilterFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

/**
 * <p>
 * Filter to indicate this call will prepare a call to trigger an event (adding a header in
 * pre-step).
 * </p>
 *
 * @author rozagerardo
 */
@Component
public class A6EventPrepareGatewayFilterFactory extends AbstractGatewayFilterFactory<Config> {

  /* default */ final Logger logger = LoggerFactory.getLogger(
      A6EventPrepareGatewayFilterFactory.class);

  private final transient GatewayFilter cacheRequestBodyFilter;

  /**
   * Main constructor injecting all the required fields.
   *
   * @param cacheRequestBodyFilter the pre-existing cache request body filter factory.
   */
  public A6EventPrepareGatewayFilterFactory(
      final CacheRequestBodyGatewayFilterFactory cacheRequestBodyFilter) {
    super(Config.class);
    final CacheRequestBodyGatewayFilterFactory.Config cacheReqBodyFilterConfig =
        new CacheRequestBodyGatewayFilterFactory.Config();
    cacheReqBodyFilterConfig.setBodyClass(ArrayNode.class);
    this.cacheRequestBodyFilter = cacheRequestBodyFilter.apply(cacheReqBodyFilterConfig);
  }

  @Override
  public GatewayFilter apply(final Config config) {
    // grab configuration from Config object
    return (exchange, chain) -> {
      // Pre logic: Set header indicating this action will trigger an event
      // (to retrieve affected contributors)
      // and trigger caching request body if Patch
      if (logger.isDebugEnabled()) {
        logger.debug("Preparing to trigger Event...");
        logger.debug(config.toString());
      }

      final boolean isPatchRequest = exchange.getRequest().getMethod().equals(HttpMethod.PATCH);
      final ServerHttpRequest.Builder builder = exchange.getRequest().mutate();
      builder.header(AngoraSixInfrastructure.TRIGGERS_EVENT_HEADER, "true");

      final ServerWebExchange updatedExchange = exchange.mutate().request(builder.build()).build();

      return isPatchRequest ? cacheRequestBodyFilter.filter(updatedExchange, chain)
          .then(chain.filter(updatedExchange))
          : chain.filter(updatedExchange);
    };
  }

  /**
   * <p>
   * Config class to use for AbstractGatewayFilterFactory.
   * </p>
   */
  public static class Config {

  }
}
