package com.angorasix.gateway.infrastructure.filters;

import com.angorasix.commons.infrastructure.constants.AngoraSixInfrastructure;
import com.angorasix.commons.infrastructure.intercommunication.events.A6InfraEventDto;
import com.angorasix.gateway.infrastructure.config.api.GatewayApiConfigurations;
import com.angorasix.gateway.infrastructure.config.constants.ConfigConstants;
import com.angorasix.gateway.infrastructure.config.internalroutes.GatewayInternalRoutesConfigurations;
import com.angorasix.gateway.infrastructure.filters.TriggerA6EventGatewayFilterFactory.Config;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.CacheRequestBodyGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.rewrite.ModifyResponseBodyGatewayFilterFactory;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
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
public class TriggerA6EventGatewayFilterFactory extends AbstractGatewayFilterFactory<Config> {

  /* default */ final Logger logger = LoggerFactory.getLogger(
      TriggerA6EventGatewayFilterFactory.class);

  private final transient GatewayInternalRoutesConfigurations internalRoutesConfigs;

  private final transient GatewayApiConfigurations apiConfigs;

  private final transient ConfigConstants configConstants;

  private final transient GatewayFilter cacheRequestBodyFilter;

  private final transient ModifyResponseBodyGatewayFilterFactory modifyResponseBodyFilter;

  private final transient String CACHED_RESPONSE_BODY_KEY = "A6-Gateway-Infrastructure-Cached-Response-Body";

  private final transient ParameterizedTypeReference<Map<String, Object>> mapType =
      new ParameterizedTypeReference<>() {
      };

  /**
   * Main constructor injecting all the required fields.
   *
   * @param apiConfigs            all api configs
   * @param internalRoutesConfigs internal routes config for events
   * @param configConstants       configuration constants
   */
  public TriggerA6EventGatewayFilterFactory(final GatewayApiConfigurations apiConfigs,
      final GatewayInternalRoutesConfigurations internalRoutesConfigs,
      final ConfigConstants configConstants,
      final ModifyResponseBodyGatewayFilterFactory modifyResponseBodyFilter,
      CacheRequestBodyGatewayFilterFactory cacheRequestBodyFilter) {
    super(Config.class);
    this.apiConfigs = apiConfigs;
    this.configConstants = configConstants;
    this.internalRoutesConfigs = internalRoutesConfigs;
    this.modifyResponseBodyFilter = modifyResponseBodyFilter;
    CacheRequestBodyGatewayFilterFactory.Config cacheReqBodyFilterConfig =
        new CacheRequestBodyGatewayFilterFactory.Config();
    cacheReqBodyFilterConfig.setBodyClass(ArrayNode.class);
    this.cacheRequestBodyFilter = cacheRequestBodyFilter.apply(cacheReqBodyFilterConfig);
  }

  @Override
  public GatewayFilter apply(Config config) {
    // grab configuration from Config object
    return (exchange, chain) -> {
      // Pre logic: Set header indicating this action will trigger an event
      // (to retrieve affected contributors)
      boolean isPatchRequest = exchange.getRequest().getMethod().equals(HttpMethod.PATCH);
      ServerHttpRequest.Builder builder = exchange.getRequest().mutate();
      builder.header(AngoraSixInfrastructure.TRIGGERS_EVENT_HEADER, "true");

      ServerWebExchange updatedExchange = exchange.mutate().request(builder.build()).build();

      GatewayFilter postWithResponseBodyFilter = modifyResponseBodyFilter.apply((c) ->
          c.setRewriteFunction(Map.class, Map.class, (filterExchange, input) ->
              ReactiveSecurityContextHolder.getContext()
                  .map(SecurityContext::getAuthentication)
                  .map(JwtAuthenticationToken.class::cast)
                  .flatMap(auth -> {
//                    ServerHttpResponse response = exchange.getResponse();
                    if (logger.isDebugEnabled()) {
                      logger.debug("Publishing Event...");
                      logger.debug(config.toString());
                    }

                    final String resolvedEventsEndpoint = internalRoutesConfigs.events()
                        .publishA6Event();
                    final String resolvedSubjectId = obtainSubjectId(exchange, config);
                    final WebClient client = WebClient.create();
                    try {
                      return client.post().uri(
                              UriComponentsBuilder.fromUriString(
                                      apiConfigs.events().baseUrl())
                                  .pathSegment(apiConfigs.events().outBasePath(),
                                      resolvedEventsEndpoint).build().toUri())
                          .header(AngoraSixInfrastructure.EVENT_AFFECTED_CONTRIBUTOR_IDS_HEADER,
                              exchange.getResponse().getHeaders().getFirst(
                                  AngoraSixInfrastructure.EVENT_AFFECTED_CONTRIBUTOR_IDS_HEADER))
                          .header(HttpHeaders.AUTHORIZATION,
                              "Bearer %s".formatted(auth.getToken().getTokenValue()))
                          .header(config.getGoogleCloudRunAuthHeader(),
                              "Bearer %s".formatted(
                                  Optional.ofNullable(exchange.getAttribute("%s-%s".formatted(
                                          configConstants.googleTokenAttribute(),
                                          apiConfigs.events().baseUrl())))
                                      .map(Object::toString)
                                      .orElse("")))
                          .bodyValue(new A6InfraEventDto(config.getSubjectType(), resolvedSubjectId,
                              obtainSubjectEvent(config, isPatchRequest, exchange.getAttribute(
                                  ServerWebExchangeUtils.CACHED_REQUEST_BODY_ATTR)),
                              input
                              // exchange.getResponse()
                          )).retrieve().onStatus(status -> status.isError(), (r) -> Mono.empty())
                          .toBodilessEntity().thenReturn(input);
                    } catch (Exception e) {
                      logger.error("LA CAGAMOOO", e);
                      return Mono.just(input);
                    }
                  })));

//      Mono<Void> internalFilterChainLogic =
      
      
      // ESTO IBA
//      return isPatchRequest ? cacheRequestBodyFilter.filter(updatedExchange, chain)
//          .then(chain.filter(updatedExchange))
//          .then(postWithResponseBodyFilter.filter(updatedExchange, chain))
//          
////          .map(() -> Mono.just(new String("ASD"))//postWithResponseBodyFilter.filter(updatedExchange, chain))
////          .then(chain.filter(updatedExchange))
          
          //CON ESsto
//          :
      return postWithResponseBodyFilter.filter(updatedExchange, chain);
//      return chain.filter(updatedExchange).then(postWithResponseBodyFilter.filter(updatedExchange, chain));


//      return postWithResponseBodyFilter.filter(updatedExchange, chain)
//          .then(chain.filter(updatedExchange));


//      return internalFilterChainLogic.then(
//          ReactiveSecurityContextHolder.getContext()
//          .map(SecurityContext::getAuthentication)
//          .map(JwtAuthenticationToken.class::cast)
//          .flatMap(auth -> {
//            ServerHttpResponse response = exchange.getResponse();
//            if (logger.isDebugEnabled()) {
//              logger.debug("Publishing Event...");
//              logger.debug(config.toString());
//            }
//
//            final String resolvedEventsEndpoint = internalRoutesConfigs.events()
//                .publishA6Event();
//            final String resolvedSubjectId = obtainSubjectId(exchange, config);
//            final WebClient client = WebClient.create();
//            try {
//              return client.post().uri(
//                      UriComponentsBuilder.fromUriString(
//                              apiConfigs.events().baseUrl())
//                          .pathSegment(apiConfigs.events().outBasePath(),
//                              resolvedEventsEndpoint).build().toUri())
//                  .header(AngoraSixInfrastructure.EVENT_AFFECTED_CONTRIBUTOR_IDS_HEADER,
//                      exchange.getResponse().getHeaders().getFirst(
//                          AngoraSixInfrastructure.EVENT_AFFECTED_CONTRIBUTOR_IDS_HEADER))
//                  .header(HttpHeaders.AUTHORIZATION,
//                      "Bearer %s".formatted(auth.getToken().getTokenValue()))
//                  .header(config.getGoogleCloudRunAuthHeader(),
//                      "Bearer %s".formatted(
//                          Optional.ofNullable(exchange.getAttribute("%s-%s".formatted(
//                                  configConstants.googleTokenAttribute(),
//                                  apiConfigs.events().baseUrl())))
//                              .map(Object::toString)
//                              .orElse("")))
//                  .bodyValue(new A6InfraEventDto(config.getSubjectType(), resolvedSubjectId,
//                      obtainSubjectEvent(config, isPatchRequest, exchange.getAttribute(
//                          ServerWebExchangeUtils.CACHED_REQUEST_BODY_ATTR)),
//                      exchange.getAttribute(CACHED_RESPONSE_BODY_KEY) // exchange.getResponse()
//                  )).retrieve().onStatus(status -> status.isError(), (r) -> Mono.empty())
//                  .toBodilessEntity().then();
//            } catch (Exception e) {
//              return Mono.empty();
//            }
//          }));
    };
  }

  private String obtainSubjectId(final ServerWebExchange exchange, Config config) {
    return Optional.ofNullable(
            exchange.getAttribute(ServerWebExchangeUtils.URI_TEMPLATE_VARIABLES_ATTRIBUTE))
        .map(Map.class::cast)
        .map(attributes -> Arrays.stream(config.getSubjectIdUriTemplateKeys().split("\\+"))
            .map(key -> attributes.get(key)).map(String.class::cast).collect(
                Collectors.joining("+")))
        .orElseThrow(() -> new IllegalArgumentException(
            "Can't obtain subjectId from request URI"));
  }

  private String obtainSubjectEvent(Config config, boolean isPatchRequest, Object requestBody) {
    if (isPatchRequest && requestBody instanceof ArrayNode arrayNode && arrayNode.size() == 1) {
      JsonNode patchOperation = arrayNode.get(0);
      String patchPath = patchOperation.get("path").asText().replaceAll("[^a-zA-Z]", "");
      String patchOp = patchOperation.get("op").asText();

      return Optional.ofNullable(config.getPatchToSubjectEventMap())
          .map(map -> map.get(patchPath))
          .map(map -> map.get(patchOp))
          .orElse(config.getDefaultSubjectEvent());
    }
    return config.getDefaultSubjectEvent();
  }

  private ServerHttpResponseDecorator getDecoratedResponse(ServerWebExchange exchange) {
    return new ServerHttpResponseDecorator(exchange.getResponse()) {

      @Override
      public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {

        if (body instanceof Flux) {

          Flux<? extends DataBuffer> fluxBody = (Flux<? extends DataBuffer>) body;

          return super.writeWith(fluxBody.buffer().map(dataBuffers -> {

            DefaultDataBuffer joinedBuffers = new DefaultDataBufferFactory().join(dataBuffers);
            byte[] content = new byte[joinedBuffers.readableByteCount()];
            joinedBuffers.read(content);
            exchange.getAttributes().put(CACHED_RESPONSE_BODY_KEY, content);
//            String responseBody = new String(content, StandardCharsets.UTF_8);

            return exchange.getResponse().bufferFactory().wrap(content);
          })).onErrorResume(err -> Mono.empty());
        }
        return super.writeWith(body);
      }
    };
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
    private Map<String, Map<String, String>> patchToSubjectEventMap = null;


    private String googleCloudRunAuthHeader =
        AngoraSixInfrastructure.GOOGLE_CLOUD_RUN_INFRA_AUTH_HEADER;

    public String getSubjectType() {
      return subjectType;
    }

    public void setSubjectType(String subjectType) {
      this.subjectType = subjectType;
    }

    public String getSubjectIdUriTemplateKeys() {
      return subjectIdUriTemplateKeys;
    }

    public void setSubjectIdUriTemplateKeys(String subjectIdUriTemplateKeys) {
      this.subjectIdUriTemplateKeys = subjectIdUriTemplateKeys;
    }

    public String getDefaultSubjectEvent() {
      return defaultSubjectEvent;
    }

    public void setDefaultSubjectEvent(String defaultSubjectEvent) {
      this.defaultSubjectEvent = defaultSubjectEvent;
    }

    public Map<String, Map<String, String>> getPatchToSubjectEventMap() {
      return patchToSubjectEventMap;
    }

    public void setPatchToSubjectEventMap(
        Map<String, Map<String, String>> patchToSubjectEventMap) {
      this.patchToSubjectEventMap = patchToSubjectEventMap;
    }

    public String getGoogleCloudRunAuthHeader() {
      return googleCloudRunAuthHeader;
    }

    public void setGoogleCloudRunAuthHeader(String googleCloudRunAuthHeader) {
      this.googleCloudRunAuthHeader = googleCloudRunAuthHeader;
    }
  }
}
