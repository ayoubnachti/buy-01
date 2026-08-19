package com.ecommerce.productservice.dtos;

public record ProductResponse(
    String id,
    String name,
    String description,
    Double price,
    String userId
) {
}
