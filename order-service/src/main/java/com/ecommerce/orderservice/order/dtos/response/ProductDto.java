package com.ecommerce.orderservice.order.dtos.response;

import java.math.BigDecimal;
import java.util.List;

public record ProductDto(
        String id,
        String name,
        String description,
        BigDecimal price,
        Integer quantity,       
        String userId,
        List<String> imageUrls) {
}
