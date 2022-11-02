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
public class ProjectsCoreInternalParams {

  private final String adminIdQueryParam;
  private final String isAdminResponseField;
  private final String projectIdResponseField;

  public ProjectsCoreInternalParams(final String adminIdQueryParam,
      final String isAdminResponseField, final String projectIdResponseField) {
    this.adminIdQueryParam = adminIdQueryParam;
    this.isAdminResponseField = isAdminResponseField;
    this.projectIdResponseField = projectIdResponseField;
  }

  public String getAdminIdQueryParam() {
    return adminIdQueryParam;
  }

  public String getIsAdminResponseField() {
    return isAdminResponseField;
  }

  public String getProjectIdResponseField() {
    return projectIdResponseField;
  }
}
