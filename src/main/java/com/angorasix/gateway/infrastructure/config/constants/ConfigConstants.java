package com.angorasix.gateway.infrastructure.config.constants;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

/**
 * <p>
 * </p>
 *
 * @author rozagerardo
 */
@ConfigurationProperties(prefix = "configs.constants")
@ConstructorBinding
public class ConfigConstants {

  private final String projectIdParam;
  private final String projectIdPlaceholder;
  private final String adminProjectIdsParam;
  private final String isProjectAdminAttribute;
  private final String projectIdsAttribute;

  public ConfigConstants(String projectIdParam, String projectIdPlaceholder,
      String adminProjectIdsParam, String isProjectAdminAttribute,
      String projectIdsAttribute) {
    this.projectIdParam = projectIdParam;
    this.projectIdPlaceholder = projectIdPlaceholder;
    this.adminProjectIdsParam = adminProjectIdsParam;
    this.isProjectAdminAttribute = isProjectAdminAttribute;
    this.projectIdsAttribute = projectIdsAttribute;
  }

  public String getProjectIdParam() {
    return projectIdParam;
  }

  public String getProjectIdPlaceholder() {
    return projectIdPlaceholder;
  }

  public String getAdminProjectIdsParam() {
    return adminProjectIdsParam;
  }

  public String getIsProjectAdminAttribute() {
    return isProjectAdminAttribute;
  }

  public String getProjectIdsAttribute() {
    return projectIdsAttribute;
  }
}
