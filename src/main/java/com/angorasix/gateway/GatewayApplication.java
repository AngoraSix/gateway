package com.angorasix.gateway;

import com.angorasix.gateway.infrastructure.config.api.GatewayApiConfigurations;
import com.angorasix.gateway.infrastructure.config.internalroutes.GatewayInternalRoutesConfigurations;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * <p>
 * Main Gateway app class, using Spring Boot features.
 * </p>
 *
 * @author rozagerardo
 */
@SpringBootApplication
@EnableConfigurationProperties({GatewayApiConfigurations.class,
    GatewayInternalRoutesConfigurations.class})
public class GatewayApplication {

  public static void main(final String[] args) {
    SpringApplication.run(GatewayApplication.class, args);
  }

}
