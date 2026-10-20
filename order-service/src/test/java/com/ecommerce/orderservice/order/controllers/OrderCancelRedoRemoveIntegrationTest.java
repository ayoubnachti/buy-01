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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Covers the H-7 test table: cancel from PENDING, cancel refused once
 * SHIPPED or already CANCELLED, ownership (404, not 403), redo only from
 * CANCELLED/DELIVERED, and remove only from CANCELLED/DELIVERED.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OrderCancelRedoRemoveIntegrationTest {

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

    private void moveStatus(String orderId, String sellerId, String newStatus) throws Exception {
        mockMvc.perform(patch("/orders/" + orderId + "/status")
                        .header("X-User-Id", sellerId)
                        .header("X-User-Role", "SELLER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"" + newStatus + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void ownerCanCancelPendingOrder_historyHasTwoEntries() throws Exception {
        String orderId = createOrderAsClient("client-1", "product-1", "seller-1");

        mockMvc.perform(post("/orders/" + orderId + "/cancel")
                        .header("X-User-Id", "client-1")
                        .header("X-User-Role", "CLIENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.statusHistory.length()").value(2));
    }

    @Test
    void cancelingTheSameOrderTwice_returns409() throws Exception {
        String orderId = createOrderAsClient("client-1", "product-1", "seller-1");

        mockMvc.perform(post("/orders/" + orderId + "/cancel")
                        .header("X-User-Id", "client-1")
                        .header("X-User-Role", "CLIENT"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/orders/" + orderId + "/cancel")
                        .header("X-User-Id", "client-1")
                        .header("X-User-Role", "CLIENT"))
                .andExpect(status().isConflict());
    }

    @Test
    void cancelingAShippedOrder_returns409() throws Exception {
        String orderId = createOrderAsClient("client-1", "product-1", "seller-1");
        moveStatus(orderId, "seller-1", "CONFIRMED");
        moveStatus(orderId, "seller-1", "SHIPPED");

        mockMvc.perform(post("/orders/" + orderId + "/cancel")
                        .header("X-User-Id", "client-1")
                        .header("X-User-Role", "CLIENT"))
                .andExpect(status().isConflict());
    }

    @Test
    void sellerCanCancelPendingOrder() throws Exception {
        String orderId = createOrderAsClient("client-1", "product-1", "seller-1");

        mockMvc.perform(post("/orders/" + orderId + "/cancel")
                        .header("X-User-Id", "seller-1")
                        .header("X-User-Role", "SELLER"))
                .andExpect(status().isOk());
    }

    @Test
    void anotherClientCannotCancel_returns404() throws Exception {
        String orderId = createOrderAsClient("client-1", "product-1", "seller-1");

        mockMvc.perform(post("/orders/" + orderId + "/cancel")
                        .header("X-User-Id", "client-2")
                        .header("X-User-Role", "CLIENT"))
                .andExpect(status().isNotFound());
    }

    @Test
    void redoCancelledOrder_createsNewPendingOrder() throws Exception {
        String orderId = createOrderAsClient("client-1", "product-1", "seller-1");
        mockMvc.perform(post("/orders/" + orderId + "/cancel")
                        .header("X-User-Id", "client-1")
                        .header("X-User-Role", "CLIENT"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/orders/" + orderId + "/redo")
                        .header("X-User-Id", "client-1")
                        .header("X-User-Role", "CLIENT"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data[0].status").value("PENDING"));
    }

    @Test
    void redoPendingOrder_returns409() throws Exception {
        String orderId = createOrderAsClient("client-1", "product-1", "seller-1");

        mockMvc.perform(post("/orders/" + orderId + "/redo")
                        .header("X-User-Id", "client-1")
                        .header("X-User-Role", "CLIENT"))
                .andExpect(status().isConflict());
    }

    @Test
    void removeDeliveredOrder_returns204() throws Exception {
        String orderId = createOrderAsClient("client-1", "product-1", "seller-1");
        moveStatus(orderId, "seller-1", "CONFIRMED");
        moveStatus(orderId, "seller-1", "SHIPPED");
        moveStatus(orderId, "seller-1", "DELIVERED");

        mockMvc.perform(delete("/orders/" + orderId)
                        .header("X-User-Id", "client-1")
                        .header("X-User-Role", "CLIENT"))
                .andExpect(status().isNoContent());
    }

    @Test
    void removePendingOrder_returns409() throws Exception {
        String orderId = createOrderAsClient("client-1", "product-1", "seller-1");

        mockMvc.perform(delete("/orders/" + orderId)
                        .header("X-User-Id", "client-1")
                        .header("X-User-Role", "CLIENT"))
                .andExpect(status().isConflict());
    }
}
