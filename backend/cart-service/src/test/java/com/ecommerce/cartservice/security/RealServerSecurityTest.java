package com.ecommerce.cartservice.security;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs on a real Tomcat. MockMvc does not perform the servlet container's /error forward,
 * so it could not catch a 403 being turned into 401 on the way out.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class RealServerSecurityTest {

    @Value("${local.server.port}")
    private int port;

    private final HttpClient http = HttpClient.newHttpClient();

    private int get(String path, String userId, String role) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
        if (userId != null) {
            request.header("X-User-Id", userId).header("X-User-Role", role);
        }
        return http.send(request.build(), HttpResponse.BodyHandlers.discarding()).statusCode();
    }

    @Test
    void sellerGets403_not401() throws Exception {
        assertThat(get("/cart", "seller-1", "SELLER")).isEqualTo(403);
    }

    @Test
    void noUserHeadersGets401() throws Exception {
        assertThat(get("/cart", null, null)).isEqualTo(401);
    }
}