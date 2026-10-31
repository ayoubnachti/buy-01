package com.ecommerce.productservice.dtos.response;

import java.util.List;

public record ProductSearchPageResponse(
    List<ProductResponse> content,
    int page,
    int size,
    long totalElements,
    int totalPages) {}
