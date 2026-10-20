package com.ecommerce.orderservice.order.controllers;

import com.ecommerce.orderservice.order.clients.ProductServiceGateway;
import com.ecommerce.orderservice.order.dtos.request.AddressRequest;
import com.ecommerce.orderservice.order.dtos.request.CreateOrderRequest;
import com.ecommerce.orderservice.order.dtos.request.OrderItemRequest;
import com.ecommerce.orderservice.order.dtos.response.ProductDto;
import com.ecommerce.orderservice.order.models.Order;
import com.ecommerce.orderservice.order.repositories.OrderRepository;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Covers the H-6 status flow test table: a seller can move one step forward,
 * cannot skip a step, a client cannot call this endpoint, and another seller
 * cannot touch an order that isn't theirs.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OrderStatusIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderRepository orderRepository;

    @MockitoBean
    private ProductServiceGateway productServiceGateway;

    private final AddressRequest address = new AddressRequest(
            "Test User", "+212600000000", "1 Main St", "Casablanca", "20000", "Morocco");

    @BeforeEach
    void cleanDatabase() {
        orderRepository.deleteAll();
    }

    private String createOrderAsClient(String clientId, String productId, String sellerId) throws Exception {
        ProductDto product = new ProductDto(
                productId, "Mouse", "A nice mouse", new BigDecimal("9.99"), 10, sellerId, List.of());
        when(productServiceGateway.getProduct(productId)).thenReturn(product);

        CreateOrderRequest request = new CreateOrderRequest(address, List.of(new OrderItemRequest(productId, 1)));

        mockMvc.perform(post("/orders")
                        .header("X-User-Id", clientId)
                        .header("X-User-Role", "CLIENT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        for (Order order : orderRepository.findAll()) {
            if (order.getUserId().equals(clientId)) {
                return order.getId();
            }
        }
        throw new IllegalStateException("Order was not created for " + clientId);
    }

    @Test
    void sellerCanMovePendingToConfirmed_andHistoryHasTheNewEntry() throws Exception {
        String orderId = createOrderAsClient("client-1", "product-1", "seller-1");

        mockMvc.perform(patch("/orders/" + orderId + "/status")
                        .header("X-User-Id", "seller-1")
                        .header("X-User-Role", "SELLER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CONFIRMED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));

        mockMvc.perform(get("/orders/" + orderId)
                        .header("X-User-Id", "seller-1")
                        .header("X-User-Role", "SELLER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.statusHistory.length()").value(2))
                .andExpect(jsonPath("$.data.statusHistory[1].status").value("CONFIRMED"));
    }

    @Test
    void sellerCannotSkipAStatus_returns409() throws Exception {
        String orderId = createOrderAsClient("client-1", "product-1", "seller-1");

        mockMvc.perform(patch("/orders/" + orderId + "/status")
                        .header("X-User-Id", "seller-1")
                        .header("X-User-Role", "SELLER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DELIVERED\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void clientCannotCallThisEndpoint_returns403() throws Exception {
        String orderId = createOrderAsClient("client-1", "product-1", "seller-1");

        mockMvc.perform(patch("/orders/" + orderId + "/status")
                        .header("X-User-Id", "client-1")
                        .header("X-User-Role", "CLIENT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CONFIRMED\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void anotherSellerCannotUpdateStatus_returns404() throws Exception {
        String orderId = createOrderAsClient("client-1", "product-1", "seller-1");

        mockMvc.perform(patch("/orders/" + orderId + "/status")
                        .header("X-User-Id", "seller-2")
                        .header("X-User-Role", "SELLER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CONFIRMED\"}"))
                .andExpect(status().isNotFound());
    }
}
