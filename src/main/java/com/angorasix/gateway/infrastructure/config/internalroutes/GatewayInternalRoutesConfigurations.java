package com.angorasix.gateway.infrastructure.config.internalroutes;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <p>
 * Base class containing all internal routes configurations.
 * </p>
 *
 * @author rozagerardo
 */
@ConfigurationProperties(prefix = "configs.internal-routes")
public record GatewayInternalRoutesConfigurations(ProjectsCoreInternalRoutes projectsCore,
                                                  ProjectsCoreInternalParams projectsCoreParams,
                                                  EventsInternalRoutes events) {

}