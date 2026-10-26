package com.ecommerce.orderservice.exceptions.orderExceptions;

public class OrderStillActiveException extends RuntimeException {
    public OrderStillActiveException(String message) {
        super(message);
    }
}
