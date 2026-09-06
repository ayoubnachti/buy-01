package com.ecommerce.userservice.controller;

import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

import java.util.List;

import com.ecommerce.userservice.dto.response.UserResponse;
import com.ecommerce.userservice.service.UserService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
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

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteUser(@PathVariable String id) {
		userService.deleteUser(id);
		return ResponseEntity.noContent().build();
	}
}
