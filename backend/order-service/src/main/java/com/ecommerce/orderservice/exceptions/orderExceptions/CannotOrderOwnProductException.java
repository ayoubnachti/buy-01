package com.ecommerce.orderservice.exceptions.orderExceptions;

public class CannotOrderOwnProductException extends RuntimeException {
    public CannotOrderOwnProductException(String message) {
        super(message);
    }
}
