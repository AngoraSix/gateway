package com.angorasix.gateway.infrastructure.filters.contributors;

import com.angorasix.gateway.infrastructure.config.api.GatewayApiConfigurations;
import com.angorasix.gateway.infrastructure.models.headers.A6ContributorHeader;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Base64;
import java.util.Collections;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * <p>
 * Filter to add Contributor Information in header.
 * </p>
 *
 * @author rozagerardo
 */
@Component
public class AddContributorHeaderGatewayFilterFactory extends
    AbstractGatewayFilterFactory<AddContributorHeaderGatewayFilterFactory.Config> {

  private String contributorHeader;

  private ObjectMapper objectMapper;

  public AddContributorHeaderGatewayFilterFactory(ObjectMapper objectMapper,
      GatewayApiConfigurations apiConfigurations) {
    super(Config.class);
    this.contributorHeader = apiConfigurations.getCommon().getContributorHeader();
    this.objectMapper = objectMapper;
  }

  @Override
  public GatewayFilter apply(final Config config) {
    return (exchange, chain) -> ReactiveSecurityContextHolder.getContext().map(
        SecurityContext::getAuthentication).map(auth -> {
      String jsonContributor;
      A6ContributorHeader a6Contributor =
          (auth instanceof JwtAuthenticationToken) ? new A6ContributorHeader(
              auth.getName(),
              ((JwtAuthenticationToken) auth).getToken().getClaim("attributes"))
              : new A6ContributorHeader(auth.getName(),
                  Collections.emptyMap());
      try {
        jsonContributor = objectMapper.writeValueAsString(a6Contributor);
      } catch (JsonProcessingException e) {
        e.printStackTrace();
        throw new RuntimeException("Error converting Principal to JSON.");
      }
      String encodedContributor = Base64.getUrlEncoder().encodeToString(jsonContributor.getBytes());
      return exchange.mutate().request(req -> req.header(contributorHeader, encodedContributor))
          .build();
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