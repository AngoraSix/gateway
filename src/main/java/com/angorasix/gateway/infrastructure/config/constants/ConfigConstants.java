package com.angorasix.gateway.infrastructure.config.constants;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <p>
 * Configs for constant aspects of the application.
 * </p>
 *
 * @author rozagerardo
 */
@ConfigurationProperties(prefix = "configs.constants")
public record ConfigConstants(String projectIdParam,
                              String mgmtIdParam,
                              String mgmtIdPlaceholder,
                              String projectIdPlaceholder,
                              String adminProjectIdsParam,
                              String isAdminAttribute,
                              String projectIdsAttribute,
                              String googleTokenAttribute,
                              String googleTokenUrlPattern,
                              String googleAudiencePlaceholder) {

}