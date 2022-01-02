package com.angorasix.gateway.infrastructure.filters.contributors;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.stereotype.Component;

/**
 * <p>
 * Filter to use Contributors Admin authorization config in downstream restricted request to
 * Contributors endpoints.
 * </p>
 *
 * @author rozagerardo
 */
@Component
public class ContributorsAdminRequestGatewayFilterFactory extends
    AbstractGatewayFilterFactory<ContributorsAdminRequestGatewayFilterFactory.Config> {

  private static final AnonymousAuthenticationToken ANONYMOUS_USER_TOKEN =
      new AnonymousAuthenticationToken(
          "anonymous", "anonymousUser",
          AuthorityUtils.createAuthorityList(new String[]{"ROLE_ANONYMOUS"}));

  private final transient ReactiveOAuth2AuthorizedClientManager authorizedClientManager;

  public ContributorsAdminRequestGatewayFilterFactory(
      final ReactiveOAuth2AuthorizedClientManager authorizedClientManager) {
    super(Config.class);
    this.authorizedClientManager = authorizedClientManager;
  }

  @Override
  public GatewayFilter apply(final Config config) {
    return (exchange, chain) -> ReactiveSecurityContextHolder.getContext()
        .map(SecurityContext::getAuthentication).defaultIfEmpty(ANONYMOUS_USER_TOKEN)
        .flatMap(principal -> {
          final OAuth2AuthorizeRequest authorizeRequest =
              OAuth2AuthorizeRequest.withClientRegistrationId(
                  "contributors").principal(principal).build();
          return this.authorizedClientManager.authorize(authorizeRequest).map(authorizedClient -> {
            final OAuth2AccessToken accessToken = authorizedClient.getAccessToken();

            exchange.getRequest().mutate()
                .header("Authorization", "Bearer " + accessToken.getTokenValue()).build();
            return exchange;
          });
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
