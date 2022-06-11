package com.angorasix.gateway.infrastructure.config.infrastructure;

import com.angorasix.gateway.infrastructure.config.internalroutes.ProjectsCoreInternalRoutes;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

/**
 * <p>
 * Base class containing all infrastructure configurations.
 * </p>
 *
 * @author rozagerardo
 */
@ConfigurationProperties(prefix = "configs.infrastructure")
@ConstructorBinding
@Getter
@AllArgsConstructor
public class InfrastructureConfigurations {

  private final ExchangeAttributes exchangeAttributes;
}
