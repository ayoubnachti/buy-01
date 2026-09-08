package com.ecommerce.mediaservice.exceptions.custom;

public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}