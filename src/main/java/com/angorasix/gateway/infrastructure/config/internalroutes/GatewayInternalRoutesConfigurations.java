package com.angorasix.gateway.infrastructure.config.internalroutes;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

/**
 * <p>
 * Base class containing all internal routes configurations.
 * </p>
 *
 * @author rozagerardo
 */
@ConfigurationProperties(prefix = "configs.internal-routes")
@ConstructorBinding
@Getter
@AllArgsConstructor
public class GatewayInternalRoutesConfigurations {

  private final ProjectsCoreInternalRoutes projectsCore;
}
