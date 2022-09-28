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

  /**
   * Simple config constructor.
   *
   * @param baseUrl     the base URL user by the Media service
   * @param inBasePath  the path used for input requests
   * @param outBasePath the path used for the downstream/output flow
   */
  public MediaApi(final String baseUrl, final String inBasePath, final String outBasePath) {
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
