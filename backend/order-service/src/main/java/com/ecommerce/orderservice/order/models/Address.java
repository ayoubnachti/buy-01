package com.ecommerce.orderservice.order.models;

public record Address(
        String fullName,
        String phone,
        String street,
        String city,
        String postalCode,
        String country) {
}