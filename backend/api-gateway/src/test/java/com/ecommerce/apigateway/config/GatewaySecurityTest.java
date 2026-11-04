package com.ecommerce.apigateway.config;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/** Real server: checks status codes and headers as a browser would see them. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = { "server.ssl.enabled=false",
    "eureka.client.enabled=false" })
class GatewaySecurityTest {

  @Value("${local.server.port}")
  private int port;

  private final HttpClient http = HttpClient.newHttpClient();

  private HttpResponse<Void> get(String path) throws Exception {
    return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).build(),
        HttpResponse.BodyHandlers.discarding());
  }

  @Test
  void protectedRouteWithoutToken_returns401() throws Exception {
    assertThat(get("/cart").statusCode()).isEqualTo(401);
  }

  @Test
  void invalidToken_returns401() throws Exception {
    HttpResponse<Void> response = http.send(
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/cart"))
            .header("Authorization", "Bearer not-a-real-token").build(),
        HttpResponse.BodyHandlers.discarding());
    assertThat(response.statusCode()).isEqualTo(401);
  }

  @Test
  void rejectedRequest_createsNoSession() throws Exception {
    assertThat(get("/cart").headers().firstValue("Set-Cookie")).isEmpty();
  }

  @Test
  void healthIsPublic() throws Exception {
    assertThat(get("/actuator/health").statusCode()).isNotIn(401, 403);
  }
}