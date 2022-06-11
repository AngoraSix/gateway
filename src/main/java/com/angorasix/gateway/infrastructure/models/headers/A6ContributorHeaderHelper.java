package com.angorasix.gateway.infrastructure.models.headers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Base64;
import java.util.Collections;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * <p>
 * </p>
 *
 * @author rozagerardo
 */
public class A6ContributorHeaderHelper {

  public static String buildAndEncodeFromAuthentication(Authentication auth,
      ObjectMapper objectMapper) {
    return A6ContributorHeaderHelper.encodeContributorHeader(
        A6ContributorHeaderHelper.buildFromAuthentication(auth), objectMapper);
  }

  public static A6ContributorHeader buildFromAuthentication(Authentication auth) {
    return (auth instanceof JwtAuthenticationToken) ? new A6ContributorHeader(
        auth.getName(),
        ((JwtAuthenticationToken) auth).getToken().getClaim("attributes"))
        : new A6ContributorHeader(auth.getName(),
            Collections.emptyMap());
  }

  public static String encodeContributorHeader(A6ContributorHeader a6Contributor,
      ObjectMapper objectMapper) {
    String jsonContributor;
    try {
      jsonContributor = objectMapper.writeValueAsString(a6Contributor);
    } catch (JsonProcessingException e) {
      e.printStackTrace();
      throw new RuntimeException("Error converting Principal to JSON.");
    }
    return Base64.getUrlEncoder()
        .encodeToString(jsonContributor.getBytes());
  }


}
