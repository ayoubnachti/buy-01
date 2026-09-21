package com.ecommerce.productservice.dtos.request;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

    public record ProductRequest(
        @NotBlank
        @Size(max = 100, message = "Name must be at most 100 characters")
        String name,

        @NotBlank
        @Size(max = 1000, message = "Description must be at most 1000 characters")
        String description,

        @NotNull @Positive BigDecimal price,
        @NotNull @Positive Integer quantity,

        @Size(max = 20, message = "A product may have at most 5 images")
        List<@NotBlank(message = "Image URL must not be blank") String> imageUrls
    ) {}