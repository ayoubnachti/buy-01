package com.ecommerce.orderservice.order.models;

import java.time.Instant;

public record StatusHistoryEntry(
        OrderStatus status,
        Instant at,
        String by) {
}