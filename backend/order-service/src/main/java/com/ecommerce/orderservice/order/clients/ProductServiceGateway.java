package com.ecommerce.orderservice.order.clients;

import com.ecommerce.orderservice.common.ResponseData;
import com.ecommerce.orderservice.exceptions.orderExceptions.OutOfStockException;
import com.ecommerce.orderservice.exceptions.orderExceptions.ProductNotFoundException;
import com.ecommerce.orderservice.exceptions.orderExceptions.ProductServiceUnavailableException;
import com.ecommerce.orderservice.order.dtos.request.StockChangeRequest;
import com.ecommerce.orderservice.order.dtos.response.ProductDto;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

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

    /**
     * Takes the ordered units out of stock: all of them, or none if one product
     * is missing or has too few left.
     */
    @CircuitBreaker(name = "productService", fallbackMethod = "stockFallback")
    public void reserveStock(Map<String, Integer> quantityByProduct) {
        try {
            productServiceClient.reserveStock(StockChangeRequest.of(quantityByProduct));
        } catch (FeignException.Conflict ex) {
            throw new OutOfStockException("Not enough stock for one of the products. Please review your order.");
        } catch (FeignException.NotFound ex) {
            throw new ProductNotFoundException("One of the products no longer exists");
        }
    }

    /** Puts units back in stock (cancelled order, or an order that could not be saved). */
    @CircuitBreaker(name = "productService", fallbackMethod = "stockFallback")
    public void releaseStock(Map<String, Integer> quantityByProduct) {
        productServiceClient.releaseStock(StockChangeRequest.of(quantityByProduct));
    }

    // Fallbacks are package-private (not private) so the unit test can call them directly.
    ProductDto getProductFallback(String productId, Throwable t) {
        log.warn("product-service unavailable for {}: {}", productId, t.getMessage());
        throw new ProductServiceUnavailableException("Products are not available now. Please try again.", t);
    }

    void stockFallback(Map<String, Integer> quantityByProduct, Throwable t) {
        // Out-of-stock and not-found are answers from product-service, not outages: pass them through.
        if (t instanceof OutOfStockException || t instanceof ProductNotFoundException) {
            throw (RuntimeException) t;
        }
        log.warn("product-service stock update failed for {}: {}", quantityByProduct.keySet(), t.getMessage());
        throw new ProductServiceUnavailableException("Products are not available now. Please try again.", t);
    }
}
