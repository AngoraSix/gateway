package com.angorasix.gateway.infrastructure.config.api;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@ConfigurationProperties(prefix = "configs.api")
@ConstructorBinding
@Getter
@AllArgsConstructor
public class GatewayApiConfigurations {

    private final ContributorsAPI contributors;
    private final ProjectsAPI projects;
}
