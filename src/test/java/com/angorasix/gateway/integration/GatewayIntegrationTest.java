package com.angorasix.gateway.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.FileCopyUtils;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:${wiremock.server.port}",
        "configs.api.projects.presentationBaseURL=http://localhost:${wiremock.server.port}"
    })
@AutoConfigureWireMock(port = 0, stubs = "classpath:/stubs/configs", files = "classpath:/stubs")
class GatewayIntegrationTest {

  @Autowired
  private WebTestClient webClient;

  @Test
  public void givenGatewayConfiguration_WhenRequestReceivedForProjectsPresentationList_ThenDirectedCorrectly() {
    // Projects Presentation base request
    webClient
        .get().uri("/projects/presentations?anyQueryParam=anyValue")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.size()").isEqualTo(2)
        .jsonPath("$[0].projectId").isEqualTo("projectId123");
  }

  @Test
  public void givenGatewayConfiguration_WhenRequestReceivedForProjectsPresentationById_ThenDirectedCorrectly() {
    // Projects Presentation request with path segments
    webClient
        .get().uri("/projects/presentations/presentationId1?anyQueryParam=anyValue")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.projectId").isEqualTo("projectId567")
        .jsonPath("$.id").isEqualTo("presentationId1");
  }
}