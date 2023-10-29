package com.angorasix.gateway.infrastructure.filters;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.rewrite.ModifyResponseBodyGatewayFilterFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Filter to replace or add User id in request.
 * </p>
 *
 * @author rozagerardo
 */
@Component
public class ComposeFieldApiGatewayFilterFactory extends
    AbstractGatewayFilterFactory<ComposeFieldApiGatewayFilterFactory.Config> {

  @Value("${server.port:9080}")
  /* default */
  private transient int serverPort;

  private final transient ModifyResponseBodyGatewayFilterFactory modifyResponseBodyFilter;

  private final transient ParameterizedTypeReference<List<Map<String, Object>>> jsonType =
      new ParameterizedTypeReference<>() {
      };

  public ComposeFieldApiGatewayFilterFactory(
      final ModifyResponseBodyGatewayFilterFactory modifyResponseBodyFilter) {
    super(Config.class);
    this.modifyResponseBodyFilter = modifyResponseBodyFilter;
  }

  @Override
  public GatewayFilter apply(final Config config) {
    return modifyResponseBodyFilter.apply((c) ->
        c.setRewriteFunction(Object.class, Object.class, (filterExchange, input) -> {
          if (!filterExchange.getResponse().getStatusCode().is2xxSuccessful() || input == null) {
            return Mono.justOrEmpty(input);
          }
          final boolean isMap = checkIsMap(input);
          final Collection<Map<String, Object>> castedInput =
              isMap ? Collections.singletonList((Map<String, Object>) input)
                  : (List<Map<String, Object>>) input;

          if (castedInput.isEmpty()) {
            return Mono.just(castedInput);
          }

          //  extract base field values (usually ids) and join them in a "," separated string
          final String baseFieldValues = extractBaseFieldValues(config, castedInput);

          final List<String> authHeader = filterExchange.getRequest().getHeaders()
              .get(HttpHeaders.AUTHORIZATION);

          // Request to a path managed by the Gateway
          final WebClient client = WebClient.create();
          return client.get()
              .uri(generateInternalUri(config, baseFieldValues))
              .header(HttpHeaders.AUTHORIZATION,
                  CollectionUtils.isEmpty(authHeader) ? "" : authHeader.get(0))
              .exchangeToMono(response -> response.bodyToMono(jsonType))
              .map(targetEntries -> processComposeRequest(targetEntries, config, castedInput,
                  isMap)
              );
        })
    );
  }

  private static boolean checkIsMap(final Object input) {
    final boolean isCollection = Collection.class.isAssignableFrom(input.getClass());
    final boolean isMap = Map.class.isAssignableFrom(input.getClass());
    if (!isCollection && !isMap) {
      throw new IllegalArgumentException(
          String.format("Trying to compose input of type [%s]", input.getClass().getName()));
    }
    return isMap;
  }

  private URI generateInternalUri(final Config config, final String baseFieldValues) {
    return UriComponentsBuilder.fromUriString("http://localhost").port(serverPort)
        .path(config.getTargetGatewayPath())
        .queryParam(config.getTargetComposeQueryParam(), baseFieldValues)
        .queryParams(CollectionUtils.toMultiValueMap(
            config.getTargetQueryParams()))
        .build().toUri();
  }

  private static String extractBaseFieldValues(final Config config,
      final Collection<Map<String, Object>> castedInput) {
    return castedInput.stream()
        .map(bodyMap -> (String) bodyMap.get(config.getOriginBaseField()))
        .collect(Collectors.joining(","));
  }

  private Object processComposeRequest(
      final List<Map<String, Object>> targetEntries,
      final Config config,
      final Collection<Map<String, Object>> castedInput,
      final boolean isMap) {
    // create a Map using the base field values as keys fo easy access
    final Map<String, Object> targetEntriesMap = targetEntries.stream().collect(
        Collectors.toMap(
            pr -> (String) pr.get(config.getTargetResponseMapField()),
            pr -> config.isListComposeField() ? Collections.singletonList(pr) : pr,
            (pr1, pr2) -> config.isListComposeField() ? Stream.of(pr1, pr2)
                .flatMap(s -> ((List) s).stream())
                .collect(Collectors.toList()) : pr1));
    // compose the origin body using the requested target entries
    final List mappedEntries = castedInput.stream().map(originEntries -> {
      originEntries.put(config.getComposeField(),
          targetEntriesMap.get(originEntries.get(config.getOriginBaseField())));
      return originEntries;
    }).collect(Collectors.toList());
    return isMap ? mappedEntries.get(0) : mappedEntries;
  }

  @Override
  public List<String> shortcutFieldOrder() {
    return Arrays.asList("originBaseField", "targetGatewayPath", "targetComposeQueryParam",
        "targetQueryParams", "targetResponseMapField", "composeField",
        "composeFieldType");
  }

  /**
   * <p>
   * Config class to use for AbstractGatewayFilterFactory.
   * </p>
   */
  public static class Config {

    private transient String originBaseField;
    private transient String targetGatewayPath;
    private transient String targetComposeQueryParam;
    private transient Map<String, List<String>> targetQueryParams;
    private transient String targetResponseMapField;
    private transient String composeField;
    private transient boolean listComposeField;

    public String getOriginBaseField() {
      return originBaseField;
    }

    public void setOriginBaseField(final String originBaseField) {
      this.originBaseField = originBaseField;
    }

    public String getTargetGatewayPath() {
      return targetGatewayPath;
    }

    public void setTargetGatewayPath(final String targetGatewayPath) {
      this.targetGatewayPath = targetGatewayPath;
    }

    public String getTargetComposeQueryParam() {
      return targetComposeQueryParam;
    }

    public void setTargetComposeQueryParam(final String targetComposeQueryParam) {
      this.targetComposeQueryParam = targetComposeQueryParam;
    }

    public String getComposeField() {
      return composeField;
    }

    public void setComposeField(final String composeField) {
      this.composeField = composeField;
    }

    public Map<String, List<String>> getTargetQueryParams() {
      return targetQueryParams;
    }

    /**
     * Setter to translate the String input to query params as Map.
     *
     * @param targetQueryParams input query params.
     */
    public void setTargetQueryParams(final String targetQueryParams) {
      this.targetQueryParams = Arrays.stream(targetQueryParams.split("&"))
          .map(param -> param.split("="))
          .collect(Collectors.toMap(p -> p[0],
              p -> Collections.singletonList(p.length > 1 ? p[1] : "")));
    }

    public String getTargetResponseMapField() {
      return targetResponseMapField;
    }

    public void setTargetResponseMapField(final String targetResponseMapField) {
      this.targetResponseMapField = targetResponseMapField;
    }

    public boolean isListComposeField() {
      return listComposeField;
    }

    public void setComposeFieldType(final String composeFieldType) {
      this.listComposeField = composeFieldType != null && "list".equals(composeFieldType);
    }
  }

}
