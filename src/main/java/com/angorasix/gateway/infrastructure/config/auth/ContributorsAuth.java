package com.angorasix.gateway.infrastructure.config.auth;

import org.springframework.boot.context.properties.ConstructorBinding;

/**
 * <p>
 * Contributors Authorization configurations.
 * </p>
 *
 * @author rozagerardo
 */
@ConstructorBinding
public class ContributorsAuth {

  private final String authKsStorePass;
  private final String authKsKeyPass;
  private final String authKsAlias;

  /**
   * <p>
   * Main constructor.
   * </p>
   *
   * @param authKsStorePass the keystore storepass
   * @param authKsKeyPass   the keystore keypass
   * @param authKsAlias     the keystore alias
   */
  public ContributorsAuth(final String authKsStorePass, final String authKsKeyPass,
      final String authKsAlias) {
    this.authKsStorePass = authKsStorePass;
    this.authKsKeyPass = authKsKeyPass;
    this.authKsAlias = authKsAlias;
  }

  public String getAuthKsKeyPass() {
    return authKsKeyPass;
  }

  public String getAuthKsStorePass() {
    return authKsStorePass;
  }

  public String getAuthKsAlias() {
    return authKsAlias;
  }
}
