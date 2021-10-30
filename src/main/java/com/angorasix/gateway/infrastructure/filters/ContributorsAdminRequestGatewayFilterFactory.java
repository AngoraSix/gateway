package com.angorasix.gateway.infrastructure.filters;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.stereotype.Component;

/**
 * <p>
 * </p>
 *
 * @author rozagerardo
 */
@Component
public class ContributorsAdminRequestGatewayFilterFactory extends
    AbstractGatewayFilterFactory<ContributorsAdminRequestGatewayFilterFactory.Config> {

  private ReactiveOAuth2AuthorizedClientManager authorizedClientManager;

  public ContributorsAdminRequestGatewayFilterFactory(
      ReactiveOAuth2AuthorizedClientManager authorizedClientManager) {
    super(Config.class);
    this.authorizedClientManager = authorizedClientManager;
  }

  @Override
  public GatewayFilter apply(Config config) {
    return (exchange, chain) ->
        ReactiveSecurityContextHolder.getContext().flatMap(authorization -> {
          OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest.withClientRegistrationId(
                  "contributors")
              .principal(authorization.getAuthentication())
              .build();
          return this.authorizedClientManager.authorize(authorizeRequest).map(authorizedClient -> {

            OAuth2AccessToken accessToken = authorizedClient.getAccessToken();

            exchange.getRequest().mutate()
                .path(exchange.getRequest().getPath() + authorization.getAuthentication().getName())
                .header("Authorization", accessToken.getTokenValue()).build();
            return exchange;
          });
        }).flatMap(chain::filter);

  }

  public static class Config {

  }

}
