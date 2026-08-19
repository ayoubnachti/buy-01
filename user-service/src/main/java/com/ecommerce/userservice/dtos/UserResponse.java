package com.ecommerce.userservice.dtos;

import com.ecommerce.userservice.User;
import com.ecommerce.userservice.UserRole;

public record UserResponse(
		String id,
		String name,
		String email,
		UserRole role) {
	public static UserResponse from(User user) {
		return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole());
	}
}