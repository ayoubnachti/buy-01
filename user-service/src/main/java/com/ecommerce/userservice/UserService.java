package com.ecommerce.userservice;

import java.util.List;

import org.springframework.stereotype.Service;

import com.ecommerce.userservice.dtos.UserResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
	private final UserRepository userRepository;

	public List<UserResponse> getAllUsers() {
		return userRepository.findAll()
			.stream()
			.map(user -> UserResponse.from(user))
			.toList()
			;
	}

	public UserResponse getUserById(String id) {
		User user = userRepository.findById(id).orElse(null);

		return UserResponse.from(user);
	}
}
