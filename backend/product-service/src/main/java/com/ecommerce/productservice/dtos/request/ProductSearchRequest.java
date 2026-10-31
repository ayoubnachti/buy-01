package com.ecommerce.productservice.dtos.request;

import java.math.BigDecimal;
import java.util.List;

import com.ecommerce.productservice.models.ProductCategory;

public record ProductSearchRequest(
    String query,
    List<ProductCategory> categories,
    BigDecimal minPrice,
    BigDecimal maxPrice,
    String sort,
    int page,
    int size) {}
