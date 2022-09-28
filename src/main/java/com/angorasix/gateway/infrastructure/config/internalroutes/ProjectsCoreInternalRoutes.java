package com.angorasix.gateway.infrastructure.config.internalroutes;

import org.springframework.boot.context.properties.ConstructorBinding;

/**
 * <p>
 * Projects Core InternalRoutes configurations, valid for Projects Core services.
 * </p>
 *
 * @author rozagerardo
 */
@ConstructorBinding
public class ProjectsCoreInternalRoutes {

  private final String adminEndpoint;

  public ProjectsCoreInternalRoutes(final String isAdminEndpoint) {
    this.adminEndpoint = isAdminEndpoint;
  }

  public String getAdminEndpoint() {
    return adminEndpoint;
  }
}
