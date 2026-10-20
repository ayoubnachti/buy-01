package com.ecommerce.orderservice.order.dtos.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CreateOrderRequest(
        @NotNull @Valid AddressRequest shippingAddress,
        @NotEmpty @Valid List<OrderItemRequest> items) {
}
