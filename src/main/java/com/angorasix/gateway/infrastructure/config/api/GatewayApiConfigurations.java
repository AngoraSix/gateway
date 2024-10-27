package com.angorasix.gateway.infrastructure.config.api;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <p>
 * Base class containing all API configurations.
 * </p>
 *
 * @author rozagerardo
 */
@ConfigurationProperties(prefix = "configs.api")
public record GatewayApiConfigurations(ContributorsApi contributors, ProjectsApi projects,
                                       ProjectManagementsApi managements,
                                       MediaApi media, EventsApi events,
                                       NotificationsApi notifications) {

}