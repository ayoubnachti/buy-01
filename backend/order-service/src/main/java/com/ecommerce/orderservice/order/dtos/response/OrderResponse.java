package com.ecommerce.orderservice.order.dtos.response;

import com.ecommerce.orderservice.order.models.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        String id,
        String userId,
        String sellerId,
        OrderStatus status,
        List<OrderItem> items,
        Address shippingAddress,
        String paymentMethod,
        BigDecimal totalAmount,
        List<StatusHistoryEntry> statusHistory,
        Instant createdAt,
        Instant updatedAt) {

    public static OrderResponse from(Order o, boolean forSeller) {
        Address address = o.getShippingAddress();
        if (forSeller && o.getStatus() == OrderStatus.CANCELLED && address != null) {
            address = new Address(address.fullName(), null, address.street(),
                    address.city(), address.postalCode(), address.country());
        }
        return new OrderResponse(o.getId(), o.getUserId(), o.getSellerId(), o.getStatus(),
                o.getItems(), address, o.getPaymentMethod(), o.getTotalAmount(),
                o.getStatusHistory(), o.getCreatedAt(), o.getUpdatedAt());
    }
}
