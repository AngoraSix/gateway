package com.angorasix.gateway.infrastructure.config.api;

/**
 * <p>
 * Projects APIs configurations, valid for Projects Core and Projects Presentation services.
 * </p>
 *
 * @author rozagerardo
 */
public record ProjectManagementsApi(ProjectManagementsCoreApi core,
                                    ProjectManagementsIntegrationApi integrations) {

}