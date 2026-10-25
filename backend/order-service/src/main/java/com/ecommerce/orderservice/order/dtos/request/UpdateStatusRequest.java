package com.ecommerce.orderservice.order.dtos.request;

import com.ecommerce.orderservice.order.models.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(@NotNull OrderStatus status) {
}
