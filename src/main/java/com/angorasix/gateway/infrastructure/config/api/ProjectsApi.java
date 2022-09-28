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

  /**
   * Simple config constructor.
   *
   * @param coreBaseUrl             the base URL user by the Project Core service
   * @param presentationBaseUrl     the base URL user by the Project Presentations service
   * @param coreInBasePath          the path used for input requests for the Projects Core service
   * @param presentationInBasePath  the path used for input requests Presentations service
   * @param coreOutBasePath         the path used for the downstream flow Core service
   * @param presentationOutBasePath the path used for the downstream flow Presentations service
   */
  public ProjectsApi(final String coreBaseUrl, final String coreInBasePath,
      final String coreOutBasePath, final String presentationBaseUrl,
      final String presentationInBasePath, final String presentationOutBasePath) {
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
