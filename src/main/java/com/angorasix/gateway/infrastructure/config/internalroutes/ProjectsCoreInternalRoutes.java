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
  private final String projectsEndpoint;

  public ProjectsCoreInternalRoutes(final String isAdminEndpoint, final String projectsEndpoint) {
    this.adminEndpoint = isAdminEndpoint;
    this.projectsEndpoint = projectsEndpoint;
  }

  public String getAdminEndpoint() {
    return adminEndpoint;
  }

  public String getProjectsEndpoint() {
    return projectsEndpoint;
  }
}
