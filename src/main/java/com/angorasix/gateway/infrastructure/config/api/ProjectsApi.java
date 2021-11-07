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
  private final String presentationBaseUrl;

  public ProjectsApi(final String coreBaseUrl, final String presentationBaseUrl) {
    this.coreBaseUrl = coreBaseUrl;
    this.presentationBaseUrl = presentationBaseUrl;
  }

  public String getCoreBaseUrl() {
    return coreBaseUrl;
  }

  public String getPresentationBaseUrl() {
    return presentationBaseUrl;
  }
}
