package com.angorasix.gateway.infrastructure.config.api;

import org.springframework.boot.context.properties.ConstructorBinding;

/**
 * @author rozagerardo
 */
@ConstructorBinding
public class ProjectsAPI {

  private final String coreBaseURL;
  private final String presentationBaseURL;

  public ProjectsAPI(String coreBaseURL, String presentationBaseURL) {
    this.coreBaseURL = coreBaseURL;
    this.presentationBaseURL = presentationBaseURL;
  }

}
