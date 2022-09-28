package com.angorasix.gateway.infrastructure.filters;

import java.util.Arrays;
import java.util.List;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

/**
 * <p>
 * Filter to replace or add User id in request.
 * </p>
 *
 * @author rozagerardo
 */
@Component
public class ModifyMethodGatewayFilterFactory extends
    AbstractGatewayFilterFactory<ModifyMethodGatewayFilterFactory.Config> {

  public ModifyMethodGatewayFilterFactory() {
    super(Config.class);
  }

  @Override
  public GatewayFilter apply(final Config config) {
    return (exchange, chain) -> {
      return chain.filter(exchange.mutate().request(req -> req.method(config.getMethod())).build());
    };
  }

  @Override
  public List<String> shortcutFieldOrder() {
    return Arrays.asList("method");
  }

  /**
   * <p>
   * Config class to use for AbstractGatewayFilterFactory.
   * </p>
   */
  public static class Config {

    private HttpMethod method;

    public HttpMethod getMethod() {
      return method;
    }

    public final void setMethod(final HttpMethod method) {
      this.method = method;
    }
  }

}
