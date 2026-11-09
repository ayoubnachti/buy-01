package com.ecommerce.productservice.dtos.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;

/** Body of the internal stock endpoints: which products, and how many units each. */
public record StockChangeRequest(@NotEmpty List<@Valid StockItem> items) {

    public record StockItem(@NotBlank String productId, @Positive int quantity) {
    }
}
