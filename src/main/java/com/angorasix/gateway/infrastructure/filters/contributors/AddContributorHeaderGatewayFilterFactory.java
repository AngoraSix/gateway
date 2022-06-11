package com.angorasix.gateway.infrastructure.filters.contributors;

import com.angorasix.gateway.infrastructure.config.api.GatewayApiConfigurations;
import com.angorasix.gateway.infrastructure.config.infrastructure.InfrastructureConfigurations;
import com.angorasix.gateway.infrastructure.models.headers.A6ContributorHeader;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

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

  private InfrastructureConfigurations infrastructureConfigs;

  // is admin checker
  private static final String PROJECT_PRESENTATION_ID_PARAM_PLACEHOLDER = ":projectId";

  ParameterizedTypeReference<Map<String, Object>> jsonType =
      new ParameterizedTypeReference<Map<String, Object>>() {
      };

  public AddContributorHeaderGatewayFilterFactory(ObjectMapper objectMapper,
      GatewayApiConfigurations apiConfigurations,
      InfrastructureConfigurations infrastructureConfigs) {
    super(Config.class);
    this.contributorHeader = apiConfigurations.getCommon().getContributorHeader();
    this.objectMapper = objectMapper;
    this.infrastructureConfigs = infrastructureConfigs;
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
          if (exchange.getAttributes()
              .containsKey(this.infrastructureConfigs.getExchangeAttributes().getIsProjectAdmin())) {
            a6Contributor.setProjectAdmin(exchange.getAttribute(
                this.infrastructureConfigs.getExchangeAttributes().getIsProjectAdmin()));
          }
          try {
            jsonContributor = objectMapper.writeValueAsString(a6Contributor);
          } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new RuntimeException("Error converting Principal to JSON.");
          }
          String encodedContributor =
              Base64.getUrlEncoder().encodeToString(jsonContributor.getBytes());
          return exchange.mutate().request(req -> req.header(contributorHeader, encodedContributor))
              .build();
        }).switchIfEmpty(Mono.just(exchange))
        .flatMap(chain::filter);

  }

  @Override
  public List<String> shortcutFieldOrder() {
    return Arrays.asList("checkIsAdmin");
  }

  public static class Config {

    private Boolean checkIsAdmin = false;

    public Config() {
    }

    public Boolean getCheckIsAdmin() {
      return checkIsAdmin;
    }

    public void setCheckIsAdmin(Boolean checkIsAdmin) {
      this.checkIsAdmin = checkIsAdmin;
    }
  }
}