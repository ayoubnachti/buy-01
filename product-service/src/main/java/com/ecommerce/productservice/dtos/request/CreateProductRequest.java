package com.ecommerce.productservice.dtos.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateProductRequest(
    @NotBlank String name,
    @NotBlank String description,
    @NotNull @Positive BigDecimal price,
    @NotNull @Min(0) Integer quantity
) {}