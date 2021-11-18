package com.angorasix.gateway.infrastructure.oauth.contributors;

import com.angorasix.gateway.infrastructure.config.auth.GatewayAuthConfigurations;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import java.security.KeyStore;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.NimbusJwtClientAuthenticationParametersConverter;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequestEntityConverter;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

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
        .clientCredentials().build();

    final DefaultReactiveOAuth2AuthorizedClientManager authorizedClientManager =
        new DefaultReactiveOAuth2AuthorizedClientManager(
        clientRegistrationRepo, authorizedClientRepo);
    authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);

    return authorizedClientManager;
  }

  /**
   * <p>
   * OAuth2AuthorizedClientManager setup to authenticate client with contributors admin configs.
   * </p>
   *
   * @param clientRegistrationRepository repository containing the client registrations
   * @param clientService                service managing client registrations
   * @param authConfig                   containing all the authorization configuration to
   *                                     authenticate as an admin contributors client
   * @return a OAuth2AuthorizedClientManager using client_credentials grant type
   */
  @Bean
  public OAuth2AuthorizedClientManager authorizedClientManager(
      ClientRegistrationRepository clientRegistrationRepository,
      OAuth2AuthorizedClientService clientService, GatewayAuthConfigurations authConfig) {
    OAuth2AuthorizedClientProvider authorizedClientProvider =
        OAuth2AuthorizedClientProviderBuilder.builder()
        .clientCredentials(
            configurer -> configurer.accessTokenResponseClient(tokenResponseClient(authConfig)))
        .build();

    AuthorizedClientServiceOAuth2AuthorizedClientManager authorizedClientManager =
        new AuthorizedClientServiceOAuth2AuthorizedClientManager(
        clientRegistrationRepository, clientService);
    authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);

    return authorizedClientManager;
  }

  private OAuth2AccessTokenResponseClient tokenResponseClient(
      GatewayAuthConfigurations authConfig) {
    Function<ClientRegistration, JWK> jwkResolver = (clientRegistration) -> {
      if (clientRegistration.getClientAuthenticationMethod()
          .equals(ClientAuthenticationMethod.PRIVATE_KEY_JWT)) {
        ClassPathResource ksFile = new ClassPathResource("a6-keystore.jks");
        try {
          KeyStore ks = KeyStore.getInstance(ksFile.getFile(),
              authConfig.getContributors().getAuthKsStorePass().toCharArray());
          RSAKey rsaKey = (RSAKey) JWK.load(ks, authConfig.getContributors().getAuthKsAlias(),
              authConfig.getContributors().getAuthKsKeyPass().toCharArray());
          RSAPublicKey publicKey = rsaKey.toRSAPublicKey();
          RSAPrivateKey privateKey = rsaKey.toRSAPrivateKey();
          return new RSAKey.Builder(publicKey).privateKey(privateKey)
              .keyID(UUID.randomUUID().toString()).build();
        } catch (Exception ex) {
          throw new IllegalStateException(
              "Contributors Client Authorization couldn't be configured", ex);
        }
      }
      return null;
    };

    OAuth2AuthorizationCodeGrantRequestEntityConverter requestEntityConverter =
        new OAuth2AuthorizationCodeGrantRequestEntityConverter();
    requestEntityConverter.addParametersConverter(
        new NimbusJwtClientAuthenticationParametersConverter<>(jwkResolver));

    DefaultAuthorizationCodeTokenResponseClient tokenResponseClient =
        new DefaultAuthorizationCodeTokenResponseClient();
    tokenResponseClient.setRequestEntityConverter(requestEntityConverter);
    return tokenResponseClient;
  }
}
