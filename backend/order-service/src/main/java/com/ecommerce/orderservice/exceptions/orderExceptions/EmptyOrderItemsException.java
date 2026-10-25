package com.ecommerce.orderservice.exceptions.orderExceptions;

public class EmptyOrderItemsException extends RuntimeException {
    public EmptyOrderItemsException(String message) {
        super(message);
    }
}
