package com.ecommerce.cartservice.dtos.response;

import java.time.Instant;
import java.util.List;

import com.ecommerce.cartservice.models.Cart;

public record CartResponse(List<Item> items, Instant updatedAt) {
    public record Item(String productId, int quantity, Instant addedAt) {}

    public static CartResponse from(Cart cart) {
        return new CartResponse(
                cart.getItems().stream()
                        .map(i -> new Item(i.getProductId(), i.getQuantity(), i.getAddedAt()))
                        .toList(),
                cart.getUpdatedAt());
    }
}