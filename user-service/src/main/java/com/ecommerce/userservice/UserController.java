package com.ecommerce.userservice;

import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.userservice.dtos.UserResponse;

import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
	private final UserService userService;

	@GetMapping
	public List<UserResponse> getUsers() {
		return userService.getAllUsers();
	}

	@GetMapping("/{id}")
	public UserResponse getUser(@PathVariable String id) {
		return userService.getUserById(id);
	}
}
