package com.ecommerce.orderservice.order.models;

import java.math.BigDecimal;

public record OrderItem(
        String productId,
        String name,
        BigDecimal unitPrice,
        int quantity,
        String imageUrl) {
}