package com.angorasix.gateway.infrastructure.models.headers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOError;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * <p>
 * Helper managing Contributor header.
 * </p>
 *
 * @author rozagerardo
 */
public final class A6ContributorHeaderHelper {

  private static final Logger LOGGER = LoggerFactory.getLogger(A6ContributorHeaderHelper.class);

  private A6ContributorHeaderHelper() {
  }

  /**
   * Helper method to create the Contributor Header out of the Authentication.
   *
   * @param auth         Security Authentication object
   * @param objectMapper objectMapper configured in the service
   * @return the encoded header
   */
  public static String buildAndEncodeFromAuthentication(final Authentication auth,
      final ObjectMapper objectMapper) {
    return A6ContributorHeaderHelper.encodeContributorHeader(
        A6ContributorHeaderHelper.buildFromAuthentication(auth), objectMapper);
  }

  /**
   * Helper method to create a Contributor header out of the Authentication object.
   *
   * @param auth Security Authentication object
   * @return the Contributor header object
   */
  public static A6ContributorHeader buildFromAuthentication(final Authentication auth) {
    return auth instanceof JwtAuthenticationToken ? new A6ContributorHeader(
        auth.getName(),
        ((JwtAuthenticationToken) auth).getToken().getClaim("attributes"))
        : new A6ContributorHeader(auth.getName(),
            Collections.emptyMap());
  }

  /**
   * Helper method to encode a contributor header using Base64.
   *
   * @param a6Contributor the Contributor header
   * @param objectMapper  the ObjectMapper configured in the service
   * @return a Base64 encoded Contributor header
   */
  public static String encodeContributorHeader(final A6ContributorHeader a6Contributor,
      final ObjectMapper objectMapper) {
    String jsonContributor;
    try {
      jsonContributor = objectMapper.writeValueAsString(a6Contributor);
    } catch (JsonProcessingException e) {
      LOGGER.error("Error converting Principal to JSON.", e);
      throw new IOError(e);
    }
    return Base64.getUrlEncoder()
        .encodeToString(jsonContributor.getBytes(StandardCharsets.UTF_8));
  }

}
