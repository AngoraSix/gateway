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

  public MediaApi(final String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public String getBaseUrl() {
    return baseUrl;
  }

}
