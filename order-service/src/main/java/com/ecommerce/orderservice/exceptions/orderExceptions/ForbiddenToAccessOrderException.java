package com.ecommerce.orderservice.exceptions.orderExceptions;

public class ForbiddenToAccessOrderException extends RuntimeException {
    public ForbiddenToAccessOrderException(String message) {
        super(message);
    }
}
