package com.angorasix.gateway.infrastructure.config;

import com.angorasix.gateway.infrastructure.filters.ContributorInfoGlobalFilter;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

//@Configuration
public class WebClientConfiguration2 {

  @Bean
  public GlobalFilter customFilter(WebClient client) {
    return new ContributorInfoGlobalFilter(client);
  }
}
