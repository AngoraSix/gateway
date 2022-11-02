package com.angorasix.gateway.infrastructure.config.api;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

/**
 * <p>
 * Base class containing all API configurations.
 * </p>
 *
 * @author rozagerardo
 */
@ConfigurationProperties(prefix = "configs.api")
@ConstructorBinding
public class GatewayApiConfigurations {

  private final ContributorsApi contributors;
  private final ProjectsApi projects;
  private final MediaApi media;
  private final CommonApi common;

  public GatewayApiConfigurations(
      ContributorsApi contributors,
      ProjectsApi projects, MediaApi media,
      CommonApi common) {
    this.contributors = contributors;
    this.projects = projects;
    this.media = media;
    this.common = common;
  }

  public ContributorsApi getContributors() {
    return contributors;
  }

  public ProjectsApi getProjects() {
    return projects;
  }

  public MediaApi getMedia() {
    return media;
  }

  public CommonApi getCommon() {
    return common;
  }
}
