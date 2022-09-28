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

  public static final String USER_ID_PLACEHOLDER = ":userId";

  private final String baseUrl;
  private final String inBasePath;
  private final String outBasePath;
  private static final String USERS_ENDPOINT = "/user";

  /**
   * Simple config constructor.
   *
   * @param baseUrl the base URL user by the Contributors service
   * @param inBasePath the path used for input requests
   * @param outBasePath the path used for the downstream/output flow
   */
  public ContributorsApi(final String baseUrl, final String inBasePath, final String outBasePath) {
    this.baseUrl = baseUrl;
    this.inBasePath = inBasePath;
    this.outBasePath = outBasePath;
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

  public String getInBasePath() {
    return inBasePath;
  }

  public String getOutBasePath() {
    return outBasePath;
  }
}
