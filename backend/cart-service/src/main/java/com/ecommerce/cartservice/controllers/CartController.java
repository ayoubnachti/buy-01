package com.ecommerce.cartservice.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.cartservice.common.ResponseData;
import com.ecommerce.cartservice.dtos.request.AddItemRequest;
import com.ecommerce.cartservice.dtos.request.UpdateQuantityRequest;
import com.ecommerce.cartservice.dtos.response.CartResponse;
import com.ecommerce.cartservice.models.Cart;
import com.ecommerce.cartservice.services.CartService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

  private final CartService cartService;

  @GetMapping
  public ResponseEntity<ResponseData<CartResponse>> getCart(@RequestHeader("X-User-Id") String userId) {
    return ok("Cart retrieved", cartService.getCart(userId));
  }

  @PostMapping("/items")
  public ResponseEntity<ResponseData<CartResponse>> addItem(@RequestHeader("X-User-Id") String userId,
      @Valid @RequestBody AddItemRequest request) {
    return ok("Item added to cart", cartService.addItem(userId, request.productId(), request.quantity()));
  }

  @PutMapping("/items/{productId}")
  public ResponseEntity<ResponseData<CartResponse>> updateQuantity(@RequestHeader("X-User-Id") String userId,
      @PathVariable String productId,
      @Valid @RequestBody UpdateQuantityRequest request) {
    return ok("Quantity updated", cartService.updateQuantity(userId, productId, request.quantity()));
  }

  @DeleteMapping("/items/{productId}")
  public ResponseEntity<ResponseData<CartResponse>> removeItem(@RequestHeader("X-User-Id") String userId,
      @PathVariable String productId) {
    return ok("Item removed from cart", cartService.removeItem(userId, productId));
  }

  @DeleteMapping
  public ResponseEntity<Void> clearCart(@RequestHeader("X-User-Id") String userId) {
    cartService.clearCart(userId);
    return ResponseEntity.noContent().build();
  }

  private ResponseEntity<ResponseData<CartResponse>> ok(String message, Cart cart) {
    return ResponseEntity.ok(ResponseData.success(message, CartResponse.from(cart)));
  }
}