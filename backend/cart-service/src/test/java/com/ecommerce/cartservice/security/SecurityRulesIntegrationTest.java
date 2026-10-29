package com.ecommerce.cartservice.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Security rules of cart-service: health is open, everything else is
 * CLIENT-only.
 * Runs without cart endpoints: 401/403 come from the security filter before any
 * controller,
 * and an allowed request with no matching controller ends as 404 from
 * GlobalExceptionHandler.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class SecurityRulesIntegrationTest {

    @Autowired
    private MockMvc mockMvc;


    @Test
    void healthIsNotBlockedBySecurity() throws Exception {
        // 200 (UP) or 503 (DOWN) both mean security let it through; only 401/403 would mean it's blocked.
        mockMvc.perform(get("/actuator/health"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotIn(401, 403));
    }

    @Test
    void cartWithoutUserHeaders_returns401() throws Exception {
        mockMvc.perform(get("/cart"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cartAsSeller_returns403() throws Exception {
        mockMvc.perform(get("/cart")
                .header("X-User-Id", "seller-1")
                .header("X-User-Role", "SELLER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void cartAsClient_passesSecurity() throws Exception {
        mockMvc.perform(get("/cart")
                .header("X-User-Id", "client-1")
                .header("X-User-Role", "CLIENT"))
                .andExpect(status().isOk());
    }
}