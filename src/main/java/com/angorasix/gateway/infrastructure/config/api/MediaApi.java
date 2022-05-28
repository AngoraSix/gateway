package com.angorasix.gateway.infrastructure.config.api;

import org.springframework.boot.context.properties.ConstructorBinding;

/**
 * <p>
 * Media APIs configurations.
 * </p>
 *
 * @author rozagerardo
 */
@ConstructorBinding
public class MediaApi {

  private final String baseUrl;
  private final String inBasePath;
  private final String outBasePath;

  public MediaApi(final String baseUrl, String inBasePath, String outBasePath) {
    this.baseUrl = baseUrl;
    this.inBasePath = inBasePath;
    this.outBasePath = outBasePath;
  }

  public String getBaseUrl() {
    return baseUrl;
  }

  public String getInBasePath() {
    return inBasePath;
  }

  public String getOutBasePath() {
    return outBasePath;
  }
}
