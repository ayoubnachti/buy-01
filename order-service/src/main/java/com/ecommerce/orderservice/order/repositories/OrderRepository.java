package com.ecommerce.orderservice.order.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.ecommerce.orderservice.order.models.Order;
import com.ecommerce.orderservice.order.models.OrderStatus;

@Repository
public interface OrderRepository extends MongoRepository<Order, String> {

    Page<Order> findByUserIdAndDeletedByUserFalse(String userId, Pageable pageable);

    Page<Order> findByUserIdAndDeletedByUserFalseAndStatus(String userId, OrderStatus status, Pageable pageable);

    Page<Order> findBySellerId(String sellerId, Pageable pageable);

    Page<Order> findBySellerIdAndStatus(String sellerId, OrderStatus status, Pageable pageable);
}