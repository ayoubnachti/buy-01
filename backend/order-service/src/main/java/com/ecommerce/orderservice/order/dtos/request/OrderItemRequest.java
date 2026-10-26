package com.ecommerce.orderservice.order.dtos.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record OrderItemRequest(
        @NotBlank String productId,
        @Min(1) @Max(99) int quantity) {
}
