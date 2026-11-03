package com.ecommerce.cartservice.dtos.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.ecommerce.cartservice.models.Cart;

public record CartResponse(List<Item> items, BigDecimal subtotal, Instant updatedAt) {

    public record Item(String productId, String name, BigDecimal price, String imageUrl, int stock,
            int quantity, boolean available, BigDecimal lineTotal, Instant addedAt) {
    }

    public static CartResponse from(Cart cart, Map<String, ProductDto> products) {
        List<Item> items = cart.getItems().stream().map(line -> {
            ProductDto p = products.get(line.getProductId());
            if (p == null) {
                return new Item(line.getProductId(), null, null, null, 0,
                        line.getQuantity(), false, null, line.getAddedAt());
            }
            BigDecimal lineTotal = p.price().multiply(BigDecimal.valueOf(line.getQuantity()));
            boolean available = p.quantity() >= line.getQuantity();
            String image = (p.imageUrls() == null || p.imageUrls().isEmpty()) ? null : p.imageUrls().get(0);
            return new Item(line.getProductId(), p.name(), p.price(), image, p.quantity(),
                    line.getQuantity(), available, lineTotal, line.getAddedAt());
        }).toList();

        BigDecimal subtotal = items.stream()
                .filter(Item::available)
                .map(Item::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartResponse(items, subtotal, cart.getUpdatedAt());
    }
}