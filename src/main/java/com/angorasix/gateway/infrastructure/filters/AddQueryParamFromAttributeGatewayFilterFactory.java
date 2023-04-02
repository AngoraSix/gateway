package com.angorasix.gateway.infrastructure.filters;

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
   * @param addRequestParamFilter filter param
   */
  public AddQueryParamFromAttributeGatewayFilterFactory(
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

    public void setAttributeField(final String attributeField) {
      this.attributeField = attributeField;
    }

    public String getQueryParamKey() {
      return queryParamKey;
    }

    public void setQueryParamKey(final String queryParamKey) {
      this.queryParamKey = queryParamKey;
    }
  }
}