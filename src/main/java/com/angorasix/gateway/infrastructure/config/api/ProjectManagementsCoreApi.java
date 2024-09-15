package com.angorasix.gateway.infrastructure.config.api;

/**
 * <p>
 * Projects Core API configs.
 * </p>
 *
 * @author rozagerardo
 */
public record ProjectManagementsCoreApi(String baseUrl,
                                        String inBasePath,
                                        String inProjectBasedPath,
                                        String outProjectBasedPath,
                                        String outBasePath) {

}
