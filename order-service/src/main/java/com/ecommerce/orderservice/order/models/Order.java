package com.ecommerce.orderservice.order.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "orders")
public class Order {

    @Id
    private String id;

    private String userId;
    private String sellerId;

    private OrderStatus status = OrderStatus.PENDING;

    private List<OrderItem> items = new ArrayList<>();
    private Address shippingAddress;
    private String paymentMethod = "PAY_ON_DELIVERY";
    private BigDecimal totalAmount;

    private List<StatusHistoryEntry> statusHistory = new ArrayList<>();
    private boolean deletedByUser = false;

    @Version
    private Long version;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public void changeStatus(OrderStatus newStatus, String by) {
        this.status = newStatus;
        this.statusHistory.add(new StatusHistoryEntry(newStatus, Instant.now(), by));
    }
}