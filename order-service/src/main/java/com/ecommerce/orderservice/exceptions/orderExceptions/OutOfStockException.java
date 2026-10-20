package com.ecommerce.orderservice.exceptions.orderExceptions;

public class OutOfStockException extends RuntimeException {
    public OutOfStockException(String message) {
        super(message);
    }
}
