package com.ecommerce.mediaservice.exceptions.profile;

public class UserIdNotFoundException extends RuntimeException {
    public UserIdNotFoundException(String message) {
        super(message);
    }
}
