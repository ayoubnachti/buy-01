package com.ecommerce.orderservice.order.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.ecommerce.orderservice.order.models.Order;

@Repository
public interface OrderRepository extends MongoRepository<Order, String> {

 
}