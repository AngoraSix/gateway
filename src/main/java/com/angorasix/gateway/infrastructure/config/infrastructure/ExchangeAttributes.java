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

  private final String projectAdmin;

  public ExchangeAttributes(final String projectAdmin) {
    this.projectAdmin = projectAdmin;
  }

  public String getProjectAdmin() {
    return projectAdmin;
  }
}
