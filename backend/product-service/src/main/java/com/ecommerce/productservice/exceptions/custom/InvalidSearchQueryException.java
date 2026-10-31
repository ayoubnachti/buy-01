package com.ecommerce.productservice.exceptions.custom;

public class InvalidSearchQueryException extends RuntimeException {
  public InvalidSearchQueryException(String message) {
    super(message);
  }
}
