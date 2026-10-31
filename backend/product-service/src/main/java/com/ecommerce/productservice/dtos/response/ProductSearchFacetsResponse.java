package com.ecommerce.productservice.dtos.response;

import java.math.BigDecimal;
import java.util.List;

import com.ecommerce.productservice.models.ProductCategory;

public record ProductSearchFacetsResponse(
    List<CategoryCount> categories,
    BigDecimal minPrice,
    BigDecimal maxPrice) {

  public record CategoryCount(ProductCategory category, long count) {}
}
