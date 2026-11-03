package com.ecommerce.cartservice.exceptions.custom;

public class OutOfStockException extends RuntimeException {
    public OutOfStockException(String productId, int available) {
        super("Only " + available + " left in stock for product " + productId);
    }
}