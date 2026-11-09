package com.ecommerce.productservice.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.productservice.dtos.request.StockChangeRequest;
import com.ecommerce.productservice.services.StockService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Stock changes made by order-service when orders are placed or cancelled.
 *
 * Service-to-service only: the API gateway routes "/products/**" but not
 * "/internal/**", and product-service's port is not published outside the
 * Docker network, so clients cannot reach these endpoints.
 */
@RestController
@RequestMapping("/internal/products/stock")
@RequiredArgsConstructor
public class InternalStockController {

    private final StockService stockService;

    /** 204 when every item was taken; 409 if one has too few left; 404 if one doesn't exist. */
    @PostMapping("/reserve")
    public ResponseEntity<Void> reserve(@Valid @RequestBody StockChangeRequest request) {
        stockService.reserve(request.items());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/release")
    public ResponseEntity<Void> release(@Valid @RequestBody StockChangeRequest request) {
        stockService.release(request.items());
        return ResponseEntity.noContent().build();
    }
}
