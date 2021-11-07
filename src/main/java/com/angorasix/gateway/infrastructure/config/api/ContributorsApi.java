package com.angorasix.gateway.infrastructure.config.api;

import org.springframework.boot.context.properties.ConstructorBinding;

/**
 * <p>
 * Contributors APIs configurations.
 * </p>
 *
 * @author rozagerardo
 */
@ConstructorBinding
public class ContributorsApi {

  private final String baseUrl;
  private static final String USERS_ENDPOINT = "/user";

  public ContributorsApi(final String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public String generateAddUserAttributeUri() {
    return this.baseUrl.concat(USERS_ENDPOINT);
  }

  public String getBaseUrl() {
    return baseUrl;
  }

  public String getUsersEndpoint() {
    return USERS_ENDPOINT;
  }
}
