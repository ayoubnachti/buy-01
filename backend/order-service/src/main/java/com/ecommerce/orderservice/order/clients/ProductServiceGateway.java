package com.ecommerce.orderservice.order.clients;

import com.ecommerce.orderservice.common.ResponseData;
import com.ecommerce.orderservice.exceptions.orderExceptions.ProductServiceUnavailableException;
import com.ecommerce.orderservice.order.dtos.response.ProductDto;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductServiceGateway {

    private final ProductServiceClient productServiceClient;

    /** Returns null when the product does not exist — the caller decides what that means. */
    @CircuitBreaker(name = "productService", fallbackMethod = "getProductFallback")
    public ProductDto getProduct(String productId) {
        try {
            ResponseData<ProductDto> response = productServiceClient.getProduct(productId);
            return response.getData() != null ? response.getData() : null;
        } catch (FeignException.NotFound ex) {
            return null;
        }
    }

    private ProductDto getProductFallback(String productId, Throwable t) {
        log.warn("product-service unavailable for {}: {}", productId, t.getMessage());
        throw new ProductServiceUnavailableException("Products are not available now. Please try again.", t);
    }
}
