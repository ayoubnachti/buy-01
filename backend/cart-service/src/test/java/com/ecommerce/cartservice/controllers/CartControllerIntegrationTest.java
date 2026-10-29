package com.ecommerce.cartservice.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.ecommerce.cartservice.repositories.CartRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles ("test")
@AutoConfigureMockMvc
class CartControllerIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private CartRepository cartRepository;

  @BeforeEach
  void cleanDatabase() {
    cartRepository.deleteAll();
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
}