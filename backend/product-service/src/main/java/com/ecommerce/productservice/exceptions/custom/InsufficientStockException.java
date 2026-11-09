package com.ecommerce.productservice.exceptions.custom;

/** Thrown when a stock decrease asks for more units than are left. Mapped to 409 Conflict. */
public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(String productId, int requested) {
        super("Not enough stock for product " + productId + ": " + requested + " requested");
    }
}