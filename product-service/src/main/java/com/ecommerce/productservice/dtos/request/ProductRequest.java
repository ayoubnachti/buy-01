package com.ecommerce.productservice.dtos.request;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ProductRequest(
    @NotBlank
    @Size(max = 100, message = "name must be at most 100 characters")
    String name,

    @NotBlank
    @Size(max = 1000, message = "description must be at most 1000 characters")
    String description,

    @NotNull @Positive BigDecimal price,
    @NotNull @Min(0) Integer quantity,

    @Size(max = 20, message = "a product may have at most 20 images")
    List<@NotBlank(message = "image URL must not be blank") String> imageUrls
) {}