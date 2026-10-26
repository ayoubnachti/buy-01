package com.ecommerce.productservice.dtos.response;

import java.math.BigDecimal;
import java.util.List;

import com.ecommerce.productservice.models.Product;
import com.ecommerce.productservice.models.ProductCategory;

public record ProductResponse(
    String id,
    String name,
    String description,
    BigDecimal price,
    Integer quantity,
    ProductCategory category,
    String userId,
    List<String> imageUrls
) {
    public static ProductResponse from(Product p, List<String> images) {
        return new ProductResponse(
            p.getId(), p.getName(), p.getDescription(),
            p.getPrice(), p.getQuantity(), p.getCategory(), p.getUserId(), images
        );
    }
}