package com.angorasix.gateway.infrastructure.filters;

import com.angorasix.commons.infrastructure.constants.AngoraSixInfrastructure;
import com.angorasix.commons.infrastructure.intercommunication.events.dto.A6InfraEventDto;
import com.angorasix.gateway.infrastructure.config.api.GatewayApiConfigurations;
import com.angorasix.gateway.infrastructure.config.constants.ConfigConstants;
import com.angorasix.gateway.infrastructure.config.internalroutes.GatewayInternalRoutesConfigurations;
import com.angorasix.gateway.infrastructure.filters.A6EventTriggerGatewayFilterFactory.Config;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.rewrite.ModifyResponseBodyGatewayFilterFactory;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Filter to indicate this call will trigger an event (adding a header in pre-step) and that handles
 * the event.
 * </p>
 *
 * @author rozagerardo
 */
@Component
public class A6EventTriggerGatewayFilterFactory extends AbstractGatewayFilterFactory<Config> {

  /* default */ final Logger logger = LoggerFactory.getLogger(
      A6EventTriggerGatewayFilterFactory.class);

  private final transient GatewayInternalRoutesConfigurations internalRoutesConfigs;

  private final transient GatewayApiConfigurations apiConfigs;

  private final transient ConfigConstants configConstants;

  private final transient ModifyResponseBodyGatewayFilterFactory respBodyFilterFactory;

  /**
   * Main constructor injecting all the required fields.
   *
   * @param apiConfigs            all api configs
   * @param internalRoutesConfigs internal routes config for events
   * @param configConstants       configuration constants
   */
  public A6EventTriggerGatewayFilterFactory(final GatewayApiConfigurations apiConfigs,
      final GatewayInternalRoutesConfigurations internalRoutesConfigs,
      final ConfigConstants configConstants,
      final ModifyResponseBodyGatewayFilterFactory respBodyFilterFactory) {
    super(Config.class);
    this.apiConfigs = apiConfigs;
    this.configConstants = configConstants;
    this.internalRoutesConfigs = internalRoutesConfigs;
    this.respBodyFilterFactory = respBodyFilterFactory;
  }

  @Override
  public GatewayFilter apply(final Config config) {
    return respBodyFilterFactory.apply((c) ->
        c.setRewriteFunction(Map.class, Map.class, (filterExchange, input) ->
            ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(JwtAuthenticationToken.class::cast)
                .flatMap(auth -> {
                  if (logger.isDebugEnabled()) {
                    logger.debug("Publishing Event...");
                    logger.debug(config.toString());
                  }

                  final boolean isPatchRequest = filterExchange.getRequest().getMethod()
                      .equals(HttpMethod.PATCH);

                  final String resolvedEventsEndpoint = internalRoutesConfigs.events()
                      .publishA6Event();
                  final String resolvedSubjectId = obtainSubjectId(filterExchange, config, input);
                  final WebClient client = WebClient.create();
                  return client.post().uri(
                          UriComponentsBuilder.fromUriString(
                                  apiConfigs.events().baseUrl())
                              .pathSegment(apiConfigs.events().outBasePath(),
                                  resolvedEventsEndpoint).build().toUri())
                      .header(AngoraSixInfrastructure.EVENT_AFFECTED_CONTRIBUTOR_IDS_HEADER,
                          filterExchange.getResponse().getHeaders().getFirst(
                              AngoraSixInfrastructure.EVENT_AFFECTED_CONTRIBUTOR_IDS_HEADER))
                      .header(HttpHeaders.AUTHORIZATION,
                          "Bearer %s".formatted(auth.getToken().getTokenValue()))
                      .header(config.getGoogleCloudRunAuthHeader(),
                          "Bearer %s".formatted(
                              Optional.ofNullable(filterExchange.getAttribute("%s-%s".formatted(
                                      configConstants.googleTokenAttribute(),
                                      apiConfigs.events().baseUrl())))
                                  .map(Object::toString)
                                  .orElse("")))
                      .bodyValue(new A6InfraEventDto(config.getSubjectType(), resolvedSubjectId,
                          obtainSubjectEvent(config, isPatchRequest, filterExchange.getAttribute(
                              ServerWebExchangeUtils.CACHED_REQUEST_BODY_ATTR)),
                          input
                      )).retrieve().onStatus(status -> status.isError(), (r) -> {
                        logger.error("Error publishing event", r);
                        return Mono.empty();
                      })
                      .toBodilessEntity().thenReturn(input);
                })));

  }

  private String obtainSubjectId(final ServerWebExchange exchange, final Config config,
      final Map<String, Object> input) {
    return Optional.ofNullable(input.get("id")).map(String.class::cast)
        .or(() -> Optional.ofNullable(
                exchange.getAttribute(ServerWebExchangeUtils.URI_TEMPLATE_VARIABLES_ATTRIBUTE))
            .map(Map.class::cast)
            .map(attributes -> Arrays.stream(config.getSubjectIdUriTemplateKeys().split("\\+"))
                .map(key -> attributes.get(key)).map(String.class::cast).collect(
                    Collectors.joining("+"))))
        .orElseThrow(() -> new IllegalArgumentException(
            "Can't obtain subjectId from request URI"));
  }

  private String obtainSubjectEvent(final Config config, final boolean isPatchRequest,
      final Object requestBody) {
    if (isPatchRequest && requestBody instanceof ArrayNode arrayNode && arrayNode.size() == 1) {
      final JsonNode patchOperation = arrayNode.get(0);
      final String patchPath = patchOperation.get("path").asText().replaceAll("[^a-zA-Z]", "");
      final String patchOp = patchOperation.get("op").asText();

      return Optional.ofNullable(config.getPatchToSubjectEventMap())
          .map(map -> map.get(patchPath))
          .map(map -> map.get(patchOp))
          .orElse(config.getDefaultSubjectEvent());
    }
    return config.getDefaultSubjectEvent();
  }

  @Override
  public List<String> shortcutFieldOrder() {
    return Arrays.asList("subjectType",
        "subjectIdUriTemplateKeys", "defaultSubjectEvent");
  }

  /**
   * <p>
   * Config class to use for AbstractGatewayFilterFactory.
   * </p>
   */
  public static class Config {

    private String subjectType;
    private String subjectIdUriTemplateKeys;
    private String defaultSubjectEvent;

    // Used to map a Patch Operation to a Subject Event
    // first level keys indicate "path" (without any special character)
    // second level keys "op"
    private Map<String, Map<String, String>> patchToSubjectEventMap;


    private String googleCloudRunAuthHeader =
        AngoraSixInfrastructure.GOOGLE_CLOUD_RUN_INFRA_AUTH_HEADER;

    public String getSubjectType() {
      return subjectType;
    }

    public void setSubjectType(final String subjectType) {
      this.subjectType = subjectType;
    }

    public String getSubjectIdUriTemplateKeys() {
      return subjectIdUriTemplateKeys;
    }

    public void setSubjectIdUriTemplateKeys(final String subjectIdUriTemplateKeys) {
      this.subjectIdUriTemplateKeys = subjectIdUriTemplateKeys;
    }

    public String getDefaultSubjectEvent() {
      return defaultSubjectEvent;
    }

    public void setDefaultSubjectEvent(final String defaultSubjectEvent) {
      this.defaultSubjectEvent = defaultSubjectEvent;
    }

    public Map<String, Map<String, String>> getPatchToSubjectEventMap() {
      return patchToSubjectEventMap;
    }

    public void setPatchToSubjectEventMap(
        final Map<String, Map<String, String>> patchToSubjectEventMap) {
      this.patchToSubjectEventMap = patchToSubjectEventMap;
    }

    public String getGoogleCloudRunAuthHeader() {
      return googleCloudRunAuthHeader;
    }

    public void setGoogleCloudRunAuthHeader(final String googleCloudRunAuthHeader) {
      this.googleCloudRunAuthHeader = googleCloudRunAuthHeader;
    }
  }
}
