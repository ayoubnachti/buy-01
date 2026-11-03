package com.ecommerce.cartservice.clients;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.ecommerce.cartservice.dtos.response.ProductDto;
import com.ecommerce.cartservice.exceptions.custom.ProductServiceUnavailableException;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductServiceGateway {

    private final ProductServiceClient productServiceClient;

    /** One product, or null when it does not exist. Used by add/update checks. */
    @CircuitBreaker(name = "productService", fallbackMethod = "getProductFallback")
    public ProductDto getProduct(String productId) {
        return fetchOrNull(productId);
    }

    @CircuitBreaker(name = "productService", fallbackMethod = "getProductsFallback")
    public Map<String, ProductDto> getProducts(Collection<String> productIds) {
        Map<String, ProductDto> products = new HashMap<>();
        for (String id : new LinkedHashSet<>(productIds)) {
            ProductDto product = fetchOrNull(id);
            if (product != null) {
                products.put(id, product);
            }
        }
        return products;
    }

    private ProductDto fetchOrNull(String productId) {
        try {
            return productServiceClient.getProduct(productId).getData();
        } catch (FeignException.NotFound ex) {
            return null;
        }
    }

    ProductDto getProductFallback(String productId, Throwable t) {
        log.warn("product-service unavailable for {}: {}", productId, t.getMessage());
        throw new ProductServiceUnavailableException("Products are not available now. Please try again.", t);
    }

    Map<String, ProductDto> getProductsFallback(Collection<String> productIds, Throwable t) {
        log.warn("product-service unavailable for {} products: {}", productIds.size(), t.getMessage());
        throw new ProductServiceUnavailableException("Products are not available now. Please try again.", t);
    }
}