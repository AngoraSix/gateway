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
 * Filter to pass Contributor Admin Authorization header downstream to access restricted
 * Contributors endpoints.
 * </p>
 *
 * @author rozagerardo
 */
@Component
public class ContributorsAdminRequestGatewayFilterFactory extends
    AbstractGatewayFilterFactory<ContributorsAdminRequestGatewayFilterFactory.Config> {

  private final transient ReactiveOAuth2AuthorizedClientManager authorizedClientManager;

  public ContributorsAdminRequestGatewayFilterFactory(
      final ReactiveOAuth2AuthorizedClientManager authorizedClientManager) {
    super(Config.class);
    this.authorizedClientManager = authorizedClientManager;
  }

  @Override
  public GatewayFilter apply(final Config config) {
    return (exchange, chain) -> ReactiveSecurityContextHolder.getContext()
        .flatMap(authorization -> {
          final OAuth2AuthorizeRequest authorizeRequest =
              OAuth2AuthorizeRequest.withClientRegistrationId(
              "contributors").principal(authorization.getAuthentication()).build();
          return this.authorizedClientManager.authorize(authorizeRequest).map(authorizedClient -> {

            final OAuth2AccessToken accessToken = authorizedClient.getAccessToken();

            exchange.getRequest().mutate()
                .path(exchange.getRequest().getPath() + authorization.getAuthentication().getName())
                .header("Authorization", accessToken.getTokenValue()).build();
            return exchange;
          });
        }).log().map(asd -> {
          System.out.println("GERGERGER");
          return asd;
        }).flatMap(chain::filter);

  }

  /**
   * <p>
   * Config class to use for AbstractGatewayFilterFactory.
   * </p>
   */
  public static class Config {

  }

}
