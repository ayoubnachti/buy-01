package com.ecommerce.orderservice.exceptions.orderExceptions;

public class OrderAlreadyShippedException extends RuntimeException {
    public OrderAlreadyShippedException(String message) {
        super(message);
    }
}
