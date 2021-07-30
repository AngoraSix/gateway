package com.angorasix.gateway.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

//@Configuration
public class WebClientConfiguration {

//  @Bean
//  WebClient client(ReactorLoadBalancerExchangeFilterFunction balancerFilter) {
//    return WebClient.builder().filter(balancerFilter).build();
//  }
  
  @Bean
  WebClient client() {
    return WebClient.builder().build();
  }
}
