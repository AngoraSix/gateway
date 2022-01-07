package com.angorasix.gateway.infrastructure.config.api;

import org.springframework.boot.context.properties.ConstructorBinding;

/**
 * <p>
 * Common configurations that are applicable to all/any API setup.
 * </p>
 *
 * @author rozagerardo
 */
@ConstructorBinding
public class CommonApi {

  private final String contributorHeader;

  public CommonApi(final String contributorHeader) {
    this.contributorHeader = contributorHeader;
  }

  public String getContributorHeader() {
    return contributorHeader;
  }
}
