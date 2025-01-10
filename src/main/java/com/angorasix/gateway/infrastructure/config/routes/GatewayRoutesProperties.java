package com.angorasix.gateway.infrastructure.config.routes;

import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.gateway.route.RouteDefinition;

/**
 * Configuration properties for the gateway routes.
 */
@ConfigurationProperties(prefix = "config.gateway")
public class GatewayRoutesProperties {

  private Map<String, List<RouteDefinition>> routes;

  public Map<String, List<RouteDefinition>> getRoutes() {
    return routes;
  }

  public void setRoutes(Map<String, List<RouteDefinition>> routes) {
    this.routes = routes;
  }
}
