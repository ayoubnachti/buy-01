package com.ecommerce.userservice;

import java.util.List;

import org.springframework.stereotype.Service;

import com.ecommerce.userservice.dtos.UserResponse;

@Service
public class UserService {
	private final List<UserResponse> users = List.of(
			new UserResponse("1", "Alice Martin", "alice@example.com"),
			new UserResponse("2", "Bob Chen", "bob@example.com"),
			new UserResponse("3", "Fatima Zahra", "fatima@example.com"));

	public List<UserResponse> getAllUsers() {
		return users;
	}

	public UserResponse getUserById(String id) {
		UserResponse user = this.users
				.stream()
				.filter(u -> u.id()
						.equals(id))
				.findFirst()
				.orElse(null);

		System.err.println(user);

		return user;
	}
}
