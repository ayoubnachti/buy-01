package com.ecommerce.cartservice.controllers;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.ecommerce.cartservice.clients.ProductServiceGateway;
import com.ecommerce.cartservice.dtos.response.ProductDto;
import com.ecommerce.cartservice.exceptions.custom.ProductServiceUnavailableException;
import com.ecommerce.cartservice.repositories.CartRepository;

import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class CartControllerIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private CartRepository cartRepository;

  // no product-service in tests. The real gateway is replaced by a mock.
  @MockitoBean
  private ProductServiceGateway productGateway;

  // check the argument order against your ProductDto record
  private static ProductDto product(String id, int stock) {
    return new ProductDto(id, "Product " + id, "desc", new BigDecimal("10.00"), stock,
        "ELECTRONICS", "seller-1", List.of("https://img/" + id + ".jpg"));
  }

  @BeforeEach
  void cleanDatabase() {
    cartRepository.deleteAll();
    when(productGateway.getProduct(anyString())).thenAnswer(inv -> product(inv.getArgument(0), 100));
    when(productGateway.getProducts(anyCollection())).thenAnswer(inv -> {
      Collection<String> ids = inv.getArgument(0);
      return ids.stream().collect(Collectors.toMap(id -> id, id -> product(id, 100)));
    });
  }

  // ---- helpers: every call goes in as a CLIENT, like the gateway would send it

  private ResultActions addItem(String userId, String productId, int quantity) throws Exception {
    return mockMvc.perform(post("/cart/items")
        .header("X-User-Id", userId)
        .header("X-User-Role", "CLIENT")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"productId\":\"" + productId + "\",\"quantity\":" + quantity + "}"));
  }

  private ResultActions getCart(String userId) throws Exception {
    return mockMvc.perform(get("/cart")
        .header("X-User-Id", userId)
        .header("X-User-Role", "CLIENT"));
  }

  private ResultActions updateQuantity(String userId, String productId, int quantity) throws Exception {
    return mockMvc.perform(put("/cart/items/" + productId)
        .header("X-User-Id", userId)
        .header("X-User-Role", "CLIENT")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"quantity\":" + quantity + "}"));
  }

  private ResultActions removeItem(String userId, String productId) throws Exception {
    return mockMvc.perform(delete("/cart/items/" + productId)
        .header("X-User-Id", userId)
        .header("X-User-Role", "CLIENT"));
  }

  // ---- the cart is stored on the server

  @Test
  void addedItem_isStillThereOnTheNextRequest() throws Exception {
    addItem("client-1", "p1", 2).andExpect(status().isOk());

    getCart("client-1")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.items.length()").value(1))
        .andExpect(jsonPath("$.data.items[0].productId").value("p1"))
        .andExpect(jsonPath("$.data.items[0].quantity").value(2));
  }

  @Test
  void getCart_withoutCart_returnsEmptyCart() throws Exception {
    getCart("client-1")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.items.length()").value(0));
  }

  @Test
  void addingSameProductTwice_mergesQuantity() throws Exception {
    addItem("client-1", "p1", 2);
    addItem("client-1", "p1", 3)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.items.length()").value(1))
        .andExpect(jsonPath("$.data.items[0].quantity").value(5));
  }

  @Test
  void updateQuantity_changesIt() throws Exception {
    addItem("client-1", "p1", 2);
    updateQuantity("client-1", "p1", 7)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.items[0].quantity").value(7));
  }

  @Test
  void removeItem_removesIt() throws Exception {
    addItem("client-1", "p1", 2);
    removeItem("client-1", "p1")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.items.length()").value(0));
  }

  @Test
  void clearCart_returns204_andCartIsEmptyAfterwards() throws Exception {
    addItem("client-1", "p1", 2);

    mockMvc.perform(delete("/cart").header("X-User-Id", "client-1").header("X-User-Role", "CLIENT"))
        .andExpect(status().isNoContent());

    getCart("client-1").andExpect(jsonPath("$.data.items.length()").value(0));
  }

  @Test
  void clearCart_withoutCart_isStill204() throws Exception {
    mockMvc.perform(delete("/cart").header("X-User-Id", "client-1").header("X-User-Role", "CLIENT"))
        .andExpect(status().isNoContent());
  }

  // ---- ownership: a user only ever reaches their own cart

  @Test
  void anotherClient_seesAnEmptyCart_notMine() throws Exception {
    addItem("client-1", "p1", 2);

    getCart("client-2").andExpect(jsonPath("$.data.items.length()").value(0));
  }

  @Test
  void anotherClient_cannotUpdateOrRemoveMyItem() throws Exception {
    addItem("client-1", "p1", 2);

    updateQuantity("client-2", "p1", 9).andExpect(status().isNotFound());
    removeItem("client-2", "p1").andExpect(status().isNotFound());

    getCart("client-1").andExpect(jsonPath("$.data.items[0].quantity").value(2)); // untouched
  }

  @Test
  void seller_cannotAddToCart() throws Exception {
    mockMvc.perform(post("/cart/items")
        .header("X-User-Id", "seller-1")
        .header("X-User-Role", "SELLER")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"productId\":\"p1\",\"quantity\":1}"))
        .andExpect(status().isForbidden());
  }

  // ---- validation and missing items

  @Test
  void addItem_withQuantityZero_returns400() throws Exception {
    addItem("client-1", "p1", 0)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false));
  }

  @Test
  void addItem_withBlankProductId_returns400() throws Exception {
    addItem("client-1", " ", 1).andExpect(status().isBadRequest());
  }

  @Test
  void updateQuantity_toZero_returns400() throws Exception {
    addItem("client-1", "p1", 2);
    updateQuantity("client-1", "p1", 0).andExpect(status().isBadRequest());
  }

  @Test
  void updateQuantity_ofProductNotInCart_returns404() throws Exception {
    addItem("client-1", "p1", 2);
    updateQuantity("client-1", "other", 3).andExpect(status().isNotFound());
  }

  // ---- live product data and product checks

  @Test
  void getCart_showsLiveProductDataAndSubtotal() throws Exception {
    addItem("client-1", "p1", 2);

    getCart("client-1")
        .andExpect(jsonPath("$.data.items[0].name").value("Product p1"))
        .andExpect(jsonPath("$.data.items[0].imageUrl").value("https://img/p1.jpg"))
        .andExpect(jsonPath("$.data.items[0].available").value(true))
        .andExpect(jsonPath("$.data.items[0].lineTotal").value(20.0))
        .andExpect(jsonPath("$.data.subtotal").value(20.0));
  }

  @Test
  void addUnknownProduct_returns404() throws Exception {
    when(productGateway.getProduct("nope")).thenReturn(null);

    addItem("client-1", "nope", 1).andExpect(status().isNotFound());
  }

  @Test
  void addMoreThanStock_returns409() throws Exception {
    when(productGateway.getProduct("p1")).thenReturn(product("p1", 2));

    addItem("client-1", "p1", 3).andExpect(status().isConflict());
  }

  @Test
  void productServiceDown_returns503() throws Exception {
    addItem("client-1", "p1", 1);
    when(productGateway.getProducts(anyCollection()))
        .thenThrow(new ProductServiceUnavailableException("down", new RuntimeException()));

    getCart("client-1")
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.success").value(false));
  }

  @Test
  void emptyCart_worksEvenWhenProductServiceIsDown() throws Exception {
    when(productGateway.getProducts(anyCollection()))
        .thenThrow(new ProductServiceUnavailableException("down", new RuntimeException()));

    getCart("client-1").andExpect(status().isOk());
  }
}