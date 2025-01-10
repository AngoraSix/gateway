package com.angorasix.gateway.infrastructure.config.routes;

import java.util.ArrayList;
import java.util.List;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionRepository;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Custom repo that loads routes from "configs.routes.*" in the YAML.
 */
@Component
public class CustomRouteDefinitionRepository implements RouteDefinitionRepository {

  private final GatewayRoutesProperties gatewayRoutesProperties;

  public CustomRouteDefinitionRepository(GatewayRoutesProperties gatewayRoutesProperties) {
    this.gatewayRoutesProperties = gatewayRoutesProperties;
  }

  @Override
  public Flux<RouteDefinition> getRouteDefinitions() {
    // Merge all service-specific route lists into one big list
    List<RouteDefinition> allRoutes = new ArrayList<>();
    if (gatewayRoutesProperties.getRoutes() != null) {
      gatewayRoutesProperties.getRoutes().forEach((serviceName, routeList) -> {
        if (routeList != null) {
          allRoutes.addAll(routeList);
        }
      });
    }

    // Return as a Flux
    return Flux.fromIterable(allRoutes);
  }

  @Override
  public Mono<Void> save(Mono<RouteDefinition> route) {
    // If you want to allow dynamic add:
    //  1) parse route
    //  2) store in some persistent place
    //  3) return Mono.empty()
    // For now, we can just throw an unsupported operation:
    return Mono.error(new UnsupportedOperationException("Not supported yet"));
  }

  @Override
  public Mono<Void> delete(Mono<String> routeId) {
    // Similar to save, implement if you want dynamic deletes
    return Mono.error(new UnsupportedOperationException("Not supported yet"));
  }
}
