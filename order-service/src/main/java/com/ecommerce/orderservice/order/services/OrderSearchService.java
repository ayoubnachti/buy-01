package com.ecommerce.orderservice.order.services;

import com.ecommerce.orderservice.common.ResponseData;
import com.ecommerce.orderservice.order.dtos.response.OrderResponse;
import com.ecommerce.orderservice.order.dtos.response.PageResponse;
import com.ecommerce.orderservice.order.models.Order;
import com.ecommerce.orderservice.order.models.OrderStatus;
import com.ecommerce.orderservice.order.repositories.OrderRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderSearchService {

    private static final int MAX_PAGE_SIZE = 50;

    private final OrderRepository orderRepository;

    public ResponseData<PageResponse<OrderResponse>> getMyOrders(String userId, OrderStatus status, int page, int size) {
        Pageable pageable = buildPageable(page, size);

        Page<Order> foundOrders;
        if (status == null) {
            foundOrders = orderRepository.findByUserIdAndDeletedByUserFalse(userId, pageable);
        } else {
            foundOrders = orderRepository.findByUserIdAndDeletedByUserFalseAndStatus(userId, status, pageable);
        }

        return buildResponse(foundOrders, false);
    }

    public ResponseData<PageResponse<OrderResponse>> getSellerOrders(String sellerId, OrderStatus status, int page, int size) {
        Pageable pageable = buildPageable(page, size);

        Page<Order> foundOrders;
        if (status == null) {
            foundOrders = orderRepository.findBySellerId(sellerId, pageable);
        } else {
            foundOrders = orderRepository.findBySellerIdAndStatus(sellerId, status, pageable);
        }

        return buildResponse(foundOrders, true);
    }

    private Pageable buildPageable(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private ResponseData<PageResponse<OrderResponse>> buildResponse(Page<Order> foundOrders, boolean forSeller) {
        List<OrderResponse> content = new ArrayList<>();
        for (Order order : foundOrders.getContent()) {
            content.add(OrderResponse.from(order, forSeller));
        }

        PageResponse<OrderResponse> pageResponse = new PageResponse<>(
                content,
                foundOrders.getNumber(),
                foundOrders.getSize(),
                foundOrders.getTotalElements(),
                foundOrders.getTotalPages());

        return ResponseData.success("Orders retrieved successfully", pageResponse);
    }
}
