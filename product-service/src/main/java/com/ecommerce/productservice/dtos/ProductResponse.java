package com.ecommerce.productservice.dtos;

import com.ecommerce.productservice.Product;

public record ProductResponse(
        String id,
        String name,
        String description,
        Double price,
        Integer quantity,
        String userId) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(product.getId(), product.getName(), product.getDescription(), product.getPrice(),
                product.getQuantity(), product.getUserId());
    }
}
