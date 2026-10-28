package com.ecommerce.cartservice.repositories;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.ecommerce.cartservice.models.Cart;

public interface CartRepository extends MongoRepository<Cart, String> {
    Optional<Cart> findByUserId(String userId);
    long deleteByUserId(String userId);
}