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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Covers the ownership and role rules from the H-5 test table: a client only
 * sees their own orders, another client never sees someone else's order
 * (404, not 403), a seller can see the order they're selling, and a wrong
 * status value or wrong role is rejected cleanly.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OrderSearchIntegrationTest {

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
    void myOrders_returnsOnlyCallersOrders() throws Exception {
        createOrderAsClient("client-1", "product-1", "seller-1");

        mockMvc.perform(get("/orders/my")
                        .header("X-User-Id", "client-1")
                        .header("X-User-Role", "CLIENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].userId").value("client-1"));

        mockMvc.perform(get("/orders/my")
                        .header("X-User-Id", "client-2")
                        .header("X-User-Role", "CLIENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(0));
    }

    @Test
    void statusFilter_returnsOnlyMatchingStatus() throws Exception {
        createOrderAsClient("client-1", "product-1", "seller-1"); // starts as PENDING

        mockMvc.perform(get("/orders/my")
                        .header("X-User-Id", "client-1")
                        .header("X-User-Role", "CLIENT")
                        .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1));

        mockMvc.perform(get("/orders/my")
                        .header("X-User-Id", "client-1")
                        .header("X-User-Role", "CLIENT")
                        .param("status", "DELIVERED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(0));
    }

    @Test
    void invalidStatus_returns400() throws Exception {
        mockMvc.perform(get("/orders/my")
                        .header("X-User-Id", "client-1")
                        .header("X-User-Role", "CLIENT")
                        .param("status", "WRONG"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getOrderById_anotherClientCannotSeeIt_returns404() throws Exception {
        String orderId = createOrderAsClient("client-1", "product-1", "seller-1");

        mockMvc.perform(get("/orders/" + orderId)
                        .header("X-User-Id", "client-2")
                        .header("X-User-Role", "CLIENT"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getOrderById_rightSellerCanSeeIt_returns200() throws Exception {
        String orderId = createOrderAsClient("client-1", "product-1", "seller-1");

        mockMvc.perform(get("/orders/" + orderId)
                        .header("X-User-Id", "seller-1")
                        .header("X-User-Role", "SELLER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sellerId").value("seller-1"));
    }

    @Test
    void sellerOrders_asClient_returns403() throws Exception {
        mockMvc.perform(get("/orders/seller")
                        .header("X-User-Id", "client-1")
                        .header("X-User-Role", "CLIENT"))
                .andExpect(status().isForbidden());
    }

    @Test
    void pageSizeIsCapped_whenRequestingMoreThanMax() throws Exception {
        createOrderAsClient("client-1", "product-1", "seller-1");

        mockMvc.perform(get("/orders/my")
                        .header("X-User-Id", "client-1")
                        .header("X-User-Role", "CLIENT")
                        .param("size", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.size").value(50));
    }
}
