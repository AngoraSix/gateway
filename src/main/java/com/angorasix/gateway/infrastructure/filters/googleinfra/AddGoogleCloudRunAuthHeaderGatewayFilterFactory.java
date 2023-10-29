package com.angorasix.gateway.infrastructure.filters.googleinfra;

import com.angorasix.commons.infrastructure.constants.AngoraSixInfrastructure;
import com.angorasix.gateway.infrastructure.config.constants.ConfigConstants;
import java.util.Arrays;
import java.util.List;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;

/**
 * <p>
 * Filter to add Admin Header from attributes.
 * </p>
 *
 * @author rozagerardo
 */
@Component
public class AddGoogleCloudRunAuthHeaderGatewayFilterFactory extends
    AbstractGatewayFilterFactory<AddGoogleCloudRunAuthHeaderGatewayFilterFactory.Config> {

  private final transient ConfigConstants configConstants;

  /**
   * Main constructor with required params.
   *
   * @param configConstants All service constant (internal) configs
   */
  public AddGoogleCloudRunAuthHeaderGatewayFilterFactory(
      final ConfigConstants configConstants) {
    super(AddGoogleCloudRunAuthHeaderGatewayFilterFactory.Config.class);
    this.configConstants = configConstants;
  }

  @Override
  public GatewayFilter apply(final AddGoogleCloudRunAuthHeaderGatewayFilterFactory.Config config) {
    return (exchange, chain) -> chain.filter(
        config.isGoogleCloudRunAuthEnabled() && exchange.getAttributes()
            .containsKey(
                configConstants.googleTokenAttribute())
            ? exchange.mutate()
            .request(req -> {
              req.header(config.getGoogleCloudRunAuthHeader(),
                  "Bearer %s".formatted(
                      exchange.getAttribute(configConstants.googleTokenAttribute())
                          .toString()));
            })
            .build() : exchange);
  }

  @Override
  public List<String> shortcutFieldOrder() {
    return Arrays.asList("googleCloudRunAuthEnabled", "googleCloudRunAuthHeader");
  }

  /**
   * <p>
   * Config class to use for AbstractGatewayFilterFactory.
   * </p>
   */
  public static class Config {

    private boolean googleCloudRunAuthEnabled;

    private String googleCloudRunAuthHeader =
        AngoraSixInfrastructure.GOOGLE_CLOUD_RUN_INFRA_AUTH_HEADER;

    public boolean isGoogleCloudRunAuthEnabled() {
      return googleCloudRunAuthEnabled;
    }

    public void setGoogleCloudRunAuthEnabled(final boolean googleCloudRunAuthEnabled) {
      this.googleCloudRunAuthEnabled = googleCloudRunAuthEnabled;
    }

    public String getGoogleCloudRunAuthHeader() {
      return googleCloudRunAuthHeader;
    }

    public void setGoogleCloudRunAuthHeader(final String googleCloudRunAuthHeader) {
      this.googleCloudRunAuthHeader = googleCloudRunAuthHeader;
    }
  }
}
