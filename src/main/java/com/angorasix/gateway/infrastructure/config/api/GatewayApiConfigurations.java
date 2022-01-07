package com.angorasix.gateway.infrastructure.config.api;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

/**
 * <p>
 * Base class containing all API configurations.
 * </p>
 *
 * @author rozagerardo
 */
@ConfigurationProperties(prefix = "configs.api")
@ConstructorBinding
@Getter
@AllArgsConstructor
public class GatewayApiConfigurations {

  private final ContributorsApi contributors;
  private final ProjectsApi projects;
  private final MediaApi media;
  private final CommonApi common;
}
