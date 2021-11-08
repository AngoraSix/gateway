package com.angorasix.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:${wiremock.server.port}",
    })
@AutoConfigureWireMock(port = 0, stubs = "classpath:/stubs/configs/init", files = "classpath:/stubs")
public class GatewayApplicationTest {

  @Test
  public void contextLoads() {
  }

}
