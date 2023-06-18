package com.angorasix.gateway.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * <p>
 * All Spring Security configuration.
 * </p>
 *
 * @author rozagerardo
 */
@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfiguration {

  /**
   * <p>
   * Security Filter Chain setup.
   * </p>
   *
   * @param http Spring's customizable ServerHttpSecurity bean
   * @return fully configured SecurityWebFilterChain
   */
  @Bean
  public SecurityWebFilterChain springSecurityFilterChain(final ServerHttpSecurity http) {
    http.authorizeExchange(exchanges -> exchanges
        .pathMatchers(HttpMethod.GET,
            "/projects/core/**",
            "/projects/*/presentations/**",
            "/projects/presentations/**",
            "/clubs/**",
            "/contributors/**",
            "/media/static/**",
            "/projects/*/management/**",
            "/projects/management/**").permitAll()
        .anyExchange().authenticated()
    ).oauth2ResourceServer().jwt();
    return http.build();
  }
}


