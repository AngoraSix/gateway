package com.angorasix.gateway.infrastructure.config.internalroutes;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

/**
 * <p>
 * Base class containing all internal routes configurations.
 * </p>
 *
 * @author rozagerardo
 */
@ConfigurationProperties(prefix = "configs.internal-routes")
@ConstructorBinding
public class GatewayInternalRoutesConfigurations {

  private final ProjectsCoreInternalRoutes projectsCore;
  private final ProjectsCoreInternalParams projectsCoreParams;

  public GatewayInternalRoutesConfigurations(
      ProjectsCoreInternalRoutes projectsCore, ProjectsCoreInternalParams projectsCoreParams) {
    this.projectsCore = projectsCore;
    this.projectsCoreParams = projectsCoreParams;
  }

  public ProjectsCoreInternalRoutes getProjectsCore() {
    return projectsCore;
  }

  public ProjectsCoreInternalParams getProjectsCoreParams() {
    return projectsCoreParams;
  }
}
