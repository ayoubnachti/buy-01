package com.ecommerce.userservice.dtos;

public record UserResponse(
    String id,
    String name,
    String email
) {
}