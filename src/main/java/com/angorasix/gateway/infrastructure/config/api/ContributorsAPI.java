package com.angorasix.gateway.infrastructure.config.api;

import org.springframework.boot.context.properties.ConstructorBinding;

/**
 * @author rozagerardo
 */
@ConstructorBinding
public class ContributorsAPI {

  private final String baseURL;
  private final String usersEndpoint = "/user";

  public ContributorsAPI(String baseURL, String usersEndpoint) {
    this.baseURL = baseURL;
  }

  public String generateAddUserAttributeUri() {
    return this.baseURL.concat(usersEndpoint);
  }

}
