package com.angorasix.gateway.infrastructure.filters;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

public class ContributorInfoGlobalFilter implements GlobalFilter, Ordered {

  private final WebClient client;

  public ContributorInfoGlobalFilter(final WebClient client) {
    this.client = client;
  }

  @Override
  public int getOrder() {
    return -1;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    // exchange.getRequest().mutate().

    return client.get()
        .uri("http://localhost:8081/contributors/ger1@test.com")
        .headers(headers -> headers.setBasicAuth("gateway", "gateway-secret"))
        .exchange()
        .log()
        .map(cr -> {
          System.out
              .println(String.format("RETRIEVED CONTRIBUTORS RESPONSE - STATUS %d - HEADERS: %s",
                  cr.rawStatusCode(), cr.headers().header("A6-Contributor").get(0)));
          exchange.getRequest()
              .mutate()
              .headers(hs -> hs.add("A6-Contributor", cr.headers().header("A6-Contributor").get(0)))
              .build();
          return exchange;
        })
        .flatMap(chain::filter);
    // return chain.filter(exchange);
  }

}
