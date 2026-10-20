package com.ecommerce.orderservice.order.controllers;

import com.ecommerce.orderservice.common.ResponseData;
import com.ecommerce.orderservice.order.dtos.request.CreateOrderRequest;
import com.ecommerce.orderservice.order.dtos.response.OrderResponse;
import com.ecommerce.orderservice.order.services.OrderService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    @PreAuthorize("hasRole('CLIENT')")
    @PostMapping
    public ResponseEntity<ResponseData<List<OrderResponse>>> create(@RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody CreateOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrders(userId, request));
    }
}
