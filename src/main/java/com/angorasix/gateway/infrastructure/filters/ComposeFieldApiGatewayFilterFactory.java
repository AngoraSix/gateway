package com.angorasix.gateway.infrastructure.filters;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
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

  public ComposeFieldApiGatewayFilterFactory() {
    super(Config.class);
  }

  @Autowired
  ModifyResponseBodyGatewayFilterFactory modifyResponseBodyFilter;

  ParameterizedTypeReference<List<Map<String, Object>>> jsonType =
      new ParameterizedTypeReference<List<Map<String, Object>>>() {
      };

  @Value("${server.port:9080}")
  int serverPort;

  @Override
  public GatewayFilter apply(final Config config) {
    return modifyResponseBodyFilter.apply((c) -> {
      c.setRewriteFunction(Object.class, Object.class, (filterExchange, input) -> {
        if (!filterExchange.getResponse().getStatusCode().is2xxSuccessful() || input == null) {
          return Mono.justOrEmpty(input);
        }
        boolean isCollection = Collection.class.isAssignableFrom(input.getClass());
        boolean isMap = Map.class.isAssignableFrom(input.getClass());
        if (!isCollection && !isMap) {
          throw new IllegalArgumentException(
              String.format("Trying to compose input of type [%s]", input.getClass().getName()));
        }
        Collection<Map<String, Object>> castedInput =
            isMap ? Collections.singletonList((Map<String, Object>) input)
                : (List<Map<String, Object>>) input;

        //  extract base field values (usually ids) and join them in a "," separated string
        String baseFieldValues = castedInput.stream()
            .map(bodyMap -> (String) bodyMap.get(config.getOriginBaseField()))
            .collect(Collectors.joining(","));

        List<String> authHeader = filterExchange.getRequest().getHeaders()
            .get(HttpHeaders.AUTHORIZATION);

        // Request to a path managed by the Gateway
        WebClient client = WebClient.create();
        return client.get()
            .uri(UriComponentsBuilder.fromUriString("http://localhost").port(serverPort)
                .path(config.getTargetGatewayPath())
                .queryParam(config.getTargetComposeQueryParam(), baseFieldValues)
                .queryParams(CollectionUtils.toMultiValueMap(
                    config.getTargetAdditionalQueryParams()))
                .build().toUri())
            .header(HttpHeaders.AUTHORIZATION,
                !CollectionUtils.isEmpty(authHeader) ? authHeader.get(0) : null)
            .exchangeToMono(response -> response.bodyToMono(jsonType)
                .map(targetEntries -> {
                  // create a Map using the base field values as keys fo easy access
                  Map<String, Object> targetEntriesMap = targetEntries.stream().collect(
                      Collectors.toMap(
                          pr -> (String) pr.get(config.getTargetResponseMappingField()),
                          pr -> config.isListComposeField() ? Collections.singletonList(pr) : pr,
                          (pr1, pr2) -> config.isListComposeField() ? Stream.of(pr1, pr2)
                              .flatMap(s -> ((List) s).stream())
                              .collect(Collectors.toList()) : pr1));
                  // compose the origin body using the requested target entries
                  List mappedEntries = castedInput.stream().map(originEntries -> {
                    originEntries.put(config.getComposeField(),
                        targetEntriesMap.get(originEntries.get(config.getOriginBaseField())));
                    return originEntries;
                  }).collect(Collectors.toList());
                  return isMap ? mappedEntries.get(0) : mappedEntries;
                })
            );
      });
    });
  }

  @Override
  public List<String> shortcutFieldOrder() {
    return Arrays.asList("originBaseField", "targetGatewayPath", "targetComposeQueryParam",
        "targetAdditionalQueryParams", "targetResponseMappingField", "composeField",
        "composeFieldType");
  }

  /**
   * <p>
   * Config class to use for AbstractGatewayFilterFactory.
   * </p>
   */
  public static class Config {

    private String originBaseField;
    private String targetGatewayPath;
    private String targetComposeQueryParam;
    private Map<String, List<String>> targetAdditionalQueryParams;
    private String targetResponseMappingField;
    private String composeField;
    private boolean isListComposeField; // composeFieldType : "list", or otherwise JSON object

    public Config() {
    }

    public String getOriginBaseField() {
      return originBaseField;
    }

    public void setOriginBaseField(String originBaseField) {
      this.originBaseField = originBaseField;
    }

    public String getTargetGatewayPath() {
      return targetGatewayPath;
    }

    public void setTargetGatewayPath(String targetGatewayPath) {
      this.targetGatewayPath = targetGatewayPath;
    }

    public String getTargetComposeQueryParam() {
      return targetComposeQueryParam;
    }

    public void setTargetComposeQueryParam(String targetComposeQueryParam) {
      this.targetComposeQueryParam = targetComposeQueryParam;
    }

    public String getComposeField() {
      return composeField;
    }

    public void setComposeField(String composeField) {
      this.composeField = composeField;
    }

    public Map<String, List<String>> getTargetAdditionalQueryParams() {
      return targetAdditionalQueryParams;
    }

    public void setTargetAdditionalQueryParams(String targetAdditionalQueryParams) {
      this.targetAdditionalQueryParams = Arrays.stream(targetAdditionalQueryParams.split("&"))
          .map(param -> param.split("="))
          .collect(Collectors.toMap(p -> p[0],
              p -> Collections.singletonList(p.length > 1 ? p[1] : "")));
    }

    public String getTargetResponseMappingField() {
      return targetResponseMappingField;
    }

    public void setTargetResponseMappingField(String targetResponseMappingField) {
      this.targetResponseMappingField = targetResponseMappingField;
    }

    public boolean isListComposeField() {
      return isListComposeField;
    }

    public void setComposeFieldType(String composeFieldType) {
      this.isListComposeField = composeFieldType != null && "list".equals(composeFieldType);
    }
  }

}
