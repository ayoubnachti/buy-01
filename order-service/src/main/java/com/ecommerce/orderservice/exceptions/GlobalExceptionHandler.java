// exception/GlobalExceptionHandler.java
package com.ecommerce.orderservice.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.ecommerce.orderservice.common.ResponseData;
import com.ecommerce.orderservice.exceptions.orderExceptions.EmptyOrderItemsException;
import com.ecommerce.orderservice.exceptions.orderExceptions.ForbiddenToAccessOrderException;
import com.ecommerce.orderservice.exceptions.orderExceptions.OrderAlreadyCancelledException;
import com.ecommerce.orderservice.exceptions.orderExceptions.OrderAlreadyDeliveredException;
import com.ecommerce.orderservice.exceptions.orderExceptions.OrderNotFoundException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ResponseData<Void>> handleOrderNotFoundException(OrderNotFoundException ex) {
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ForbiddenToAccessOrderException.class)
    public ResponseEntity<ResponseData<Void>> handleForbiddenToAccessOrderException(ForbiddenToAccessOrderException ex) {
        return buildError(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(OrderAlreadyCancelledException.class)
    public ResponseEntity<ResponseData<Void>> handleOrderAlreadyCancelledException(OrderAlreadyCancelledException ex) {
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(OrderAlreadyDeliveredException.class)
    public ResponseEntity<ResponseData<Void>> handleOrderAlreadyDeliveredException(OrderAlreadyDeliveredException ex) {
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(EmptyOrderItemsException.class)
    public ResponseEntity<ResponseData<Void>> handleEmptyOrderItemsException(EmptyOrderItemsException ex) {
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    private ResponseEntity<ResponseData<Void>> buildError(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ResponseData.error(message));
    }
}