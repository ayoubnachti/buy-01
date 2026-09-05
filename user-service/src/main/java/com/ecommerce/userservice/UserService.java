package com.ecommerce.userservice;

import java.util.List;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.ecommerce.userservice.dtos.UserResponse;
import com.ecommerce.userservice.events.UserDeletedEvent;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
	private final UserRepository userRepository;
	private final KafkaTemplate<String, UserDeletedEvent> kafkaTemplate;

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

	public void deleteUser(String id) {
		userRepository.deleteById(id);
		kafkaTemplate.send("user-events", new UserDeletedEvent(id));
	}
}
