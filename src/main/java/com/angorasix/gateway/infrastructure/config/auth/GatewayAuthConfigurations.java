package com.angorasix.gateway.infrastructure.config.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

/**
 * <p>
 * Base class containing all Authorization configurations.
 * </p>
 *
 * @author rozagerardo
 */
@ConfigurationProperties(prefix = "configs.auth")
@ConstructorBinding
@Getter
@AllArgsConstructor
public class GatewayAuthConfigurations {

  private final ContributorsAuth contributors;
}
