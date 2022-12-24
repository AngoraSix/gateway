package com.angorasix.gateway.infrastructure.config.constants;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <p>
 * </p>
 *
 * @author rozagerardo
 */
@ConfigurationProperties(prefix = "configs.constants")
public record ConfigConstants(String projectIdParam, String projectIdPlaceholder,
                              String adminProjectIdsParam, String isProjectAdminAttribute,
                              String projectIdsAttribute) {

}