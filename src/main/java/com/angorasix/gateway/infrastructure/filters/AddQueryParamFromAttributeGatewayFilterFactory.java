package com.angorasix.gateway.infrastructure.filters;

import com.angorasix.gateway.infrastructure.config.api.GatewayApiConfigurations;
import com.angorasix.gateway.infrastructure.config.constants.ConfigConstants;
import com.angorasix.gateway.infrastructure.config.internalroutes.GatewayInternalRoutesConfigurations;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.List;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.AddRequestParameterGatewayFilterFactory;
import org.springframework.stereotype.Component;

/**
 * <p>
 * Filter to request Administered Projects and add them as a exchange attribute for next filters.
 * </p>
 *
 * @author rozagerardo
 */
@Component
public class AddQueryParamFromAttributeGatewayFilterFactory extends
    AbstractGatewayFilterFactory<AddQueryParamFromAttributeGatewayFilterFactory.Config> {

  private final transient AddRequestParameterGatewayFilterFactory addRequestParamFilter;

  /**
   * Main constructor with required params.
   *
   * @param objectMapper          the ObjectMapper configured in the service
   * @param apiConfigs            API configs
   * @param internalRoutesConfigs internal Routes configs to route composing call
   */
  public AddQueryParamFromAttributeGatewayFilterFactory(final ObjectMapper objectMapper,
      final GatewayApiConfigurations apiConfigs,
      final GatewayInternalRoutesConfigurations internalRoutesConfigs,
      final ConfigConstants configConstants,
      final AddRequestParameterGatewayFilterFactory addRequestParamFilter) {
    super(Config.class);
    this.addRequestParamFilter = addRequestParamFilter;
  }

  @Override
  public GatewayFilter apply(final Config config) {
    return (filterExchange, chain) ->
        addRequestParamFilter.apply((a) -> {
          a.setName(config.queryParamKey);
          final Object queryObject = filterExchange.getAttribute(config.attributeField);
          final String queryValue = queryObject instanceof String ? (String) queryObject
              : queryObject instanceof List ? String.join(",", (List) queryObject)
                  : queryObject.toString();
          a.setValue(queryValue);
        }).filter(filterExchange, chain);
  }

  @Override
  public List<String> shortcutFieldOrder() {
    return Arrays.asList("attributeField", "queryParamKey");
  }

  /**
   * <p>
   * Config class to use for AbstractGatewayFilterFactory.
   * </p>
   */
  public static class Config {

    private String attributeField = "";
    private String queryParamKey = "";

    public String getAttributeField() {
      return attributeField;
    }

    public void setAttributeField(String attributeField) {
      this.attributeField = attributeField;
    }

    public String getQueryParamKey() {
      return queryParamKey;
    }

    public void setQueryParamKey(String queryParamKey) {
      this.queryParamKey = queryParamKey;
    }
  }
}