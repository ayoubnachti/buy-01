package com.ecommerce.productservice.client;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.ecommerce.productservice.dtos.UserResponse;

@Service
public class UserClient {
  private final RestClient restClient;

  public UserClient(@LoadBalanced RestClient.Builder loadBalanced) {
    this.restClient = loadBalanced.baseUrl("http://user-service").build();
  }

  public UserResponse getSeller(String id) {
    return this.restClient.get()
        .uri("/users/{id}", id)
        .retrieve()
        .body(UserResponse.class);
  }
}
