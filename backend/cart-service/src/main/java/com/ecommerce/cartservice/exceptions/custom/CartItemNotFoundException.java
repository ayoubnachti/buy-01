package com.ecommerce.cartservice.exceptions.custom;

public class CartItemNotFoundException extends RuntimeException {
    public CartItemNotFoundException(String productId) {
        super("Product " + productId + " is not in your cart");
    }
}