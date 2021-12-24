package com.angorasix.gateway.infrastructure.filters;

import static com.angorasix.gateway.infrastructure.config.api.ContributorsApi.USER_ID_PLACEHOLDER;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.stereotype.Component;

/**
 * <p>
 * Filter to replace or add User id in request.
 * </p>
 *
 * @author rozagerardo
 */
@Component
public class UserScopedRequestGatewayFilterFactory extends
    AbstractGatewayFilterFactory<UserScopedRequestGatewayFilterFactory.Config> {

  private final transient ReactiveOAuth2AuthorizedClientManager authorizedClientManager;

  public UserScopedRequestGatewayFilterFactory(
      final ReactiveOAuth2AuthorizedClientManager authorizedClientManager) {
    super(Config.class);
    this.authorizedClientManager = authorizedClientManager;
  }

  @Override
  public GatewayFilter apply(final Config config) {
    return (exchange, chain) -> ReactiveSecurityContextHolder.getContext().map(authorization -> {
      final String requestPath = exchange.getRequest().getPath().value();
      final String userScopedPath =
          requestPath.contains(USER_ID_PLACEHOLDER) ? requestPath.replace(USER_ID_PLACEHOLDER,
              authorization.getAuthentication().getName())
              : exchange.getRequest().getPath() + "/" + authorization.getAuthentication().getName();

      return exchange.mutate().request(req -> req.path(userScopedPath)).build();
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
