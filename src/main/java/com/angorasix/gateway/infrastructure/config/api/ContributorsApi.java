package com.angorasix.gateway.infrastructure.config.api;

import org.springframework.boot.context.properties.ConstructorBinding;

/**
 * <p>
 * Contributors APIs configurations.
 * </p>
 *
 * @author rozagerardo
 */
public record ContributorsApi(String baseUrl, String inBasePath, String outBasePath) {

  public static final String USER_ID_PLACEHOLDER = ":userId";
  private static final String USERS_ENDPOINT = "/user";

  public String generateAddUserAttributeUri() {
    return this.baseUrl.concat(USERS_ENDPOINT);
  }
  public String getUsersEndpoint() {
    return USERS_ENDPOINT;
  }
}
