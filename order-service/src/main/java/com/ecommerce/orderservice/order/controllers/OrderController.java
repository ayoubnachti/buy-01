package com.ecommerce.orderservice.order.controllers;

import com.ecommerce.orderservice.common.ResponseData;
import com.ecommerce.orderservice.order.dtos.request.CreateOrderRequest;
import com.ecommerce.orderservice.order.dtos.request.UpdateStatusRequest;
import com.ecommerce.orderservice.order.dtos.response.OrderResponse;
import com.ecommerce.orderservice.order.dtos.response.PageResponse;
import com.ecommerce.orderservice.order.models.OrderStatus;
import com.ecommerce.orderservice.order.services.OrderSearchService;
import com.ecommerce.orderservice.order.services.OrderService;

import jakarta.validation.Valid;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;
    private final OrderSearchService searchService;

    @PreAuthorize("hasRole('CLIENT')")
    @PostMapping
    public ResponseEntity<ResponseData<List<OrderResponse>>> create(@RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody CreateOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrders(userId, request));
    }

    @PreAuthorize("hasRole('CLIENT')")
    @GetMapping("/my")
    public ResponseEntity<ResponseData<PageResponse<OrderResponse>>> myOrders(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(searchService.search("userId", userId, true, status, from, to, q, page, size, false));
    }

    @PreAuthorize("hasRole('SELLER')")
    @GetMapping("/seller")
    public ResponseEntity<ResponseData<PageResponse<OrderResponse>>> sellerOrders(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(searchService.search("sellerId", userId, false, status, from, to, q, page, size, true));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResponseData<OrderResponse>> getOne(@PathVariable String id,
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(orderService.getOrder(id, userId));
    }

    @PreAuthorize("hasRole('SELLER')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<ResponseData<OrderResponse>> updateStatus(@PathVariable String id,
            @RequestHeader("X-User-Id") String sellerId,
            @Valid @RequestBody UpdateStatusRequest request) {
        return ResponseEntity.ok(orderService.updateStatusAsSeller(id, sellerId, request.status()));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ResponseData<OrderResponse>> cancel(@PathVariable String id,
            @RequestHeader("X-User-Id") String callerId) {
        return ResponseEntity.ok(orderService.cancel(id, callerId));
    }

    @PreAuthorize("hasRole('CLIENT')")
    @PostMapping("/{id}/redo")
    public ResponseEntity<ResponseData<List<OrderResponse>>> redo(@PathVariable String id,
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.redo(id, userId));
    }

    @PreAuthorize("hasRole('CLIENT')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseData<Void>> remove(@PathVariable String id,
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(orderService.remove(id, userId));
    }
}
