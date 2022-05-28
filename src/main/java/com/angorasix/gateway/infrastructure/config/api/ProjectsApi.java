package com.angorasix.gateway.infrastructure.config.api;

import org.springframework.boot.context.properties.ConstructorBinding;

/**
 * <p>
 * Projects APIs configurations, valid for Projects Core and Projects Presentation services.
 * </p>
 *
 * @author rozagerardo
 */
@ConstructorBinding
public class ProjectsApi {

  private final String coreBaseUrl;
  private final String coreInBasePath;
  private final String coreOutBasePath;
  private final String presentationBaseUrl;
  private final String presentationInBasePath;
  private final String presentationOutBasePath;

  public ProjectsApi(final String coreBaseUrl, String coreInBasePath,
      String coreOutBasePath, final String presentationBaseUrl,
      String presentationInBasePath, String presentationOutBasePath) {
    this.coreBaseUrl = coreBaseUrl;
    this.coreInBasePath = coreInBasePath;
    this.coreOutBasePath = coreOutBasePath;
    this.presentationBaseUrl = presentationBaseUrl;
    this.presentationInBasePath = presentationInBasePath;
    this.presentationOutBasePath = presentationOutBasePath;
  }

  public String getCoreBaseUrl() {
    return coreBaseUrl;
  }

  public String getPresentationBaseUrl() {
    return presentationBaseUrl;
  }

  public String getCoreInBasePath() {
    return coreInBasePath;
  }

  public String getCoreOutBasePath() {
    return coreOutBasePath;
  }

  public String getPresentationInBasePath() {
    return presentationInBasePath;
  }

  public String getPresentationOutBasePath() {
    return presentationOutBasePath;
  }
}
