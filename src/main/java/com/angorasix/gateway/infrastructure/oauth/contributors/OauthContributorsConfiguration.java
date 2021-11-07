package com.angorasix.gateway.infrastructure.oauth.contributors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;

/**
 * <p>
 * Contributors OAuth2-related configurations.
 * </p>
 *
 * @author rozagerardo
 */
@Configuration
public class OauthContributorsConfiguration {

  /**
   * <p>
   * OAuth2AuthorizedClientManager setup.
   * </p>
   *
   * @param clientRegistrationRepo Spring Security's Repository containing all ClientRegistrations
   * @param authorizedClientRepo   Spring Security's Repository containing all authorized clients
   *                               info
   * @return a fully configured OAuth2 Authorized Client Manager
   */
  @Bean
  public ReactiveOAuth2AuthorizedClientManager authorizedClientManager(
      final ReactiveClientRegistrationRepository clientRegistrationRepo,
      final ServerOAuth2AuthorizedClientRepository authorizedClientRepo) {

    final ReactiveOAuth2AuthorizedClientProvider authorizedClientProvider =
        ReactiveOAuth2AuthorizedClientProviderBuilder.builder()
            .clientCredentials()
            .build();

    final DefaultReactiveOAuth2AuthorizedClientManager authorizedClientManager =
        new DefaultReactiveOAuth2AuthorizedClientManager(
            clientRegistrationRepo, authorizedClientRepo);
    authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);

    return authorizedClientManager;
  }
}
