package com.ecommerce.userservice.repository;

import com.ecommerce.userservice.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserRepository extends MongoRepository<User, String> {
}