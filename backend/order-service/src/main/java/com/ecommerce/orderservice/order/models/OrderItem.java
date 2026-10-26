package com.ecommerce.orderservice.order.models;

import java.math.BigDecimal;

public record OrderItem(
        String productId,
        String name,
        String category,
        BigDecimal unitPrice,
        int quantity,
        String imageUrl) {
}