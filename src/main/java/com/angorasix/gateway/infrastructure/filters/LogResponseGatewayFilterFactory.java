package com.angorasix.gateway.infrastructure.filters;

import com.angorasix.gateway.infrastructure.filters.LogResponseGatewayFilterFactory.Config;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.rewrite.ModifyResponseBodyGatewayFilterFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * <p>Filter logging response data.</p>
 *
 * @author rozagerardo
 */
@Component
public class LogResponseGatewayFilterFactory extends AbstractGatewayFilterFactory<Config> {

  /* default */ final Logger logger = LoggerFactory.getLogger(
      LogResponseGatewayFilterFactory.class);

  private final transient ModifyResponseBodyGatewayFilterFactory modifyResponseBodyFilter;

  /**
   * Main constructor with required params.
   *
   * @param modifyResponseBodyFilter delegating Modify Request Body Filter
   */
  public LogResponseGatewayFilterFactory(
      final ModifyResponseBodyGatewayFilterFactory modifyResponseBodyFilter) {
    super(Config.class);
    this.modifyResponseBodyFilter = modifyResponseBodyFilter;
  }

  @Override
  public GatewayFilter apply(final Config config) {
    return modifyResponseBodyFilter.apply(
        (c) -> c.setRewriteFunction(String.class, String.class, (filterExchange, input) -> {
          if (logger.isDebugEnabled()) {
            logger.debug("Logging response data...");
            logger.debug("STATUS:");
            logger.debug(filterExchange.getResponse().getStatusCode().toString());
            logger.debug("HEADERS:");
            logger.debug(filterExchange.getResponse().getHeaders().toString());
            logger.debug("BODY:");
            logger.debug(input);
          }
          return Mono.just(input);
        }));
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

  }
}
