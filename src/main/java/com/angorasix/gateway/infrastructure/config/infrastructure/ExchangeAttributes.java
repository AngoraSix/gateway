package com.angorasix.gateway.infrastructure.config.infrastructure;

import org.springframework.boot.context.properties.ConstructorBinding;

/**
 * <p>
 * Infrastructure exchange attribute configurations.
 * </p>
 *
 * @author rozagerardo
 */
@ConstructorBinding
public class ExchangeAttributes {

  private final String isProjectAdmin;

  public ExchangeAttributes(final String isProjectAdmin) {
    this.isProjectAdmin = isProjectAdmin;
  }

  public String getIsProjectAdmin() {
    return isProjectAdmin;
  }
}
