package com.angorasix.gateway.infrastructure.filters.googleinfra;


import com.angorasix.gateway.infrastructure.config.constants.ConfigConstants;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * <p>Filter to vobtain an ID Token to service-to-service communication in a Google Cloud Run
 * environment.</p>
 * <p>The Config.googleIdTokenUrlPattern property should contain a ':audience' placeholder</p>
 *
 * @author rozagerardo
 */
@Component
public class ProcessGoogleCloudRunAuthGatewayFilterFactory extends
    AbstractGatewayFilterFactory<ProcessGoogleCloudRunAuthGatewayFilterFactory.Config> {

  /* default */ final Logger logger = LoggerFactory.getLogger(
      ProcessGoogleCloudRunAuthGatewayFilterFactory.class);

  private final transient ConfigConstants configConstants;

  /**
   * Main constructor with required params.
   *
   * @param configConstants All service constant (internal) configs
   */
  public ProcessGoogleCloudRunAuthGatewayFilterFactory(final ConfigConstants configConstants) {
    super(Config.class);
    this.configConstants = configConstants;
  }

  @Override
  public GatewayFilter apply(final Config config) {
    return (exchange, chain) -> {

      return (config.isGoogleCloudRunAuthEnabled() ? processGoogleCloudRunAuth(config, exchange)
          : Mono.just(exchange)).flatMap(chain::filter);
    };
  }

  private Mono<ServerWebExchange> processGoogleCloudRunAuth(final Config config,
      final ServerWebExchange exchange) {
    if (logger.isDebugEnabled()) {
      logger.debug("Processing Google Cloud Run Auth...");
      logger.debug(config.toString());
    }
    final String audience;
    if (StringUtils.hasText(config.getAudience())) {
      audience = config.getAudience();
    } else {
      final URI currentUri = ((Route) exchange.getAttribute(
          ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR)).getUri();
      audience = "%s://%s".formatted(currentUri.getScheme(), currentUri.getHost());
    }
    if (logger.isDebugEnabled()) {
      logger.debug("For audience: %s".formatted(audience));
    }
    return obtainIdTokenForAudience(audience,
        configConstants.googleTokenUrlPattern(),
        configConstants.googleAudiencePlaceholder())
        .map(idToken -> {
          if (logger.isDebugEnabled()) {
            logger.debug("Obtained Google Cloud Run ID Token...");
          }
          exchange.getAttributes()
              .put("%s-%s".formatted(configConstants.googleTokenAttribute(), audience),
                  idToken);
          if (!StringUtils.hasText(config.getAudience())) {
            exchange.getAttributes()
                .put(configConstants.googleTokenAttribute(), idToken);
          }
          return exchange;
        });
  }

  private static Mono<String> obtainIdTokenForAudience(final String audience,
      final String googleIdTokenUrlPattern,
      final String audiencePlaceholder) {
    return WebClient.create().get()
        .uri(googleIdTokenUrlPattern.replace(audiencePlaceholder, audience))
        .header("Metadata-Flavor", "Google")
        .exchangeToMono(response -> response.bodyToMono(String.class));
  }

  @Override
  public List<String> shortcutFieldOrder() {
    return Arrays.asList("googleCloudRunAuthEnabled", "audience");
  }

  /**
   * <p>
   * Config class to use for AbstractGatewayFilterFactory.
   * </p>
   */
  public static class Config {

    private boolean googleCloudRunAuthEnabled;

    private String audience;

    public boolean isGoogleCloudRunAuthEnabled() {
      return googleCloudRunAuthEnabled;
    }

    public void setGoogleCloudRunAuthEnabled(final boolean googleCloudRunAuthEnabled) {
      this.googleCloudRunAuthEnabled = googleCloudRunAuthEnabled;
    }

    public String getAudience() {
      return audience;
    }

    public void setAudience(final String audience) {
      this.audience = audience;
    }
  }
}