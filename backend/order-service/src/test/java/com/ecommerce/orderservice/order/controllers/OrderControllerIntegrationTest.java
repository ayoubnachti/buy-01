package com.ecommerce.orderservice.order.controllers;

import com.ecommerce.orderservice.order.clients.ProductServiceGateway;
import com.ecommerce.orderservice.order.dtos.request.AddressRequest;
import com.ecommerce.orderservice.order.dtos.request.CreateOrderRequest;
import com.ecommerce.orderservice.order.dtos.request.OrderItemRequest;
import com.ecommerce.orderservice.order.dtos.response.ProductDto;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductServiceGateway productServiceGateway;

    private final AddressRequest address = new AddressRequest(
            "Test User", "+212600000000", "1 Main St", "Casablanca", "20000", "Morocco");

    @Test
    void createOrder_withValidClientRequest_returns201AndSavesOrder() throws Exception {
        ProductDto product = new ProductDto(
                "product-1", "Keyboard", "A nice keyboard",
                new BigDecimal("19.99"), 10, "ELECTRONICS", "seller-1", List.of());
        when(productServiceGateway.getProduct("product-1")).thenReturn(product);

        CreateOrderRequest request = new CreateOrderRequest(
                address, List.of(new OrderItemRequest("product-1", 2)));

        mockMvc.perform(post("/orders")
                        .header("X-User-Id", "client-1")
                        .header("X-User-Role", "CLIENT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].sellerId").value("seller-1"))
                .andExpect(jsonPath("$.data[0].totalAmount").value(39.98));
    }

    @Test
    void createOrder_withSellerRole_returns403() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(
                address, List.of(new OrderItemRequest("product-1", 1)));

        mockMvc.perform(post("/orders")
                        .header("X-User-Id", "seller-1")
                        .header("X-User-Role", "SELLER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createOrder_withEmptyItems_returns400() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(address, List.of());

        mockMvc.perform(post("/orders")
                        .header("X-User-Id", "client-1")
                        .header("X-User-Role", "CLIENT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
