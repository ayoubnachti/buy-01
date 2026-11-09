package com.ecommerce.productservice.services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.ecommerce.productservice.dtos.request.StockChangeRequest.StockItem;
import com.ecommerce.productservice.exceptions.custom.InsufficientStockException;
import com.ecommerce.productservice.exceptions.custom.ResourceNotFoundException;
import com.ecommerce.productservice.repositories.ProductRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockService {

    private final ProductRepository productRepository;

    /**
     * Takes stock for every item, or for none of them: if one product is missing
     * or has too few left, the items already taken are given back before the
     * error is thrown.
     */
    public void reserve(List<StockItem> items) {
        List<StockItem> done = new ArrayList<>();
        try {
            for (StockItem item : items) {
                decrease(item.productId(), item.quantity());
                done.add(item);
            }
        } catch (RuntimeException ex) {
            for (StockItem item : done) {
                productRepository.increaseQuantity(item.productId(), item.quantity());
            }
            throw ex;
        }
    }

    /**
     * Gives stock back (cancelled order). A product deleted since the order was
     * placed has nothing to give back to, so it is skipped instead of failing the
     * cancel.
     */
    public void release(List<StockItem> items) {
        for (StockItem item : items) {
            if (productRepository.increaseQuantity(item.productId(), item.quantity()) == 0) {
                log.info("Stock release skipped: product {} no longer exists", item.productId());
            }
        }
    }

    public void decrease(String productId, int quantity) {
        long changed = productRepository.decreaseQuantityIfEnough(productId, quantity, -quantity);

        if (changed == 0) {
            // Nothing matched: either no such product, or not enough left. Tell them apart for the caller.
            if (!productRepository.existsById(productId)) {
                throw new ResourceNotFoundException("Product " + productId + " not found");
            }
            throw new InsufficientStockException(productId, quantity);
        }
    }
}
