// exception/GlobalExceptionHandler.java
package com.ecommerce.orderservice.exceptions;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.ecommerce.orderservice.common.ResponseData;
import com.ecommerce.orderservice.exceptions.orderExceptions.CannotOrderOwnProductException;
import com.ecommerce.orderservice.exceptions.orderExceptions.EmptyOrderItemsException;
import com.ecommerce.orderservice.exceptions.orderExceptions.ForbiddenToAccessOrderException;
import com.ecommerce.orderservice.exceptions.orderExceptions.InvalidOrderStatusTransitionException;
import com.ecommerce.orderservice.exceptions.orderExceptions.OrderAlreadyCancelledException;
import com.ecommerce.orderservice.exceptions.orderExceptions.OrderAlreadyDeliveredException;
import com.ecommerce.orderservice.exceptions.orderExceptions.OrderAlreadyShippedException;
import com.ecommerce.orderservice.exceptions.orderExceptions.OrderNotFoundException;
import com.ecommerce.orderservice.exceptions.orderExceptions.OrderStillActiveException;
import com.ecommerce.orderservice.exceptions.orderExceptions.OutOfStockException;
import com.ecommerce.orderservice.exceptions.orderExceptions.ProductNotFoundException;
import com.ecommerce.orderservice.exceptions.orderExceptions.ProductServiceUnavailableException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ResponseData<Void>> handleOrderNotFoundException(OrderNotFoundException ex) {
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ResponseData<Void>> handleProductNotFoundException(ProductNotFoundException ex) {
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ForbiddenToAccessOrderException.class)
    public ResponseEntity<ResponseData<Void>> handleForbiddenToAccessOrderException(ForbiddenToAccessOrderException ex) {
        return buildError(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(EmptyOrderItemsException.class)
    public ResponseEntity<ResponseData<Void>> handleEmptyOrderItemsException(EmptyOrderItemsException ex) {
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(CannotOrderOwnProductException.class)
    public ResponseEntity<ResponseData<Void>> handleCannotOrderOwnProductException(CannotOrderOwnProductException ex) {
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(OutOfStockException.class)
    public ResponseEntity<ResponseData<Void>> handleOutOfStockException(OutOfStockException ex) {
        return buildError(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(ProductServiceUnavailableException.class)
    public ResponseEntity<ResponseData<Void>> handleProductServiceUnavailableException(ProductServiceUnavailableException ex) {
        return buildError(HttpStatus.BAD_GATEWAY, ex.getMessage());
    }

    @ExceptionHandler(InvalidOrderStatusTransitionException.class)
    public ResponseEntity<ResponseData<Void>> handleInvalidOrderStatusTransitionException(InvalidOrderStatusTransitionException ex) {
        return buildError(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(OrderAlreadyCancelledException.class)
    public ResponseEntity<ResponseData<Void>> handleOrderAlreadyCancelledException(OrderAlreadyCancelledException ex) {
        return buildError(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(OrderAlreadyDeliveredException.class)
    public ResponseEntity<ResponseData<Void>> handleOrderAlreadyDeliveredException(OrderAlreadyDeliveredException ex) {
        return buildError(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(OrderAlreadyShippedException.class)
    public ResponseEntity<ResponseData<Void>> handleOrderAlreadyShippedException(OrderAlreadyShippedException ex) {
        return buildError(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(OrderStillActiveException.class)
    public ResponseEntity<ResponseData<Void>> handleOrderStillActiveException(OrderStillActiveException ex) {
        return buildError(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ResponseData<Void>> handleOptimisticLockingFailureException(OptimisticLockingFailureException ex) {
        return buildError(HttpStatus.CONFLICT, "This order was changed by someone else. Reload and try again.");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ResponseData<Void>> handleMethodNotAllowedException(Exception ex) {
        return buildError(HttpStatus.METHOD_NOT_ALLOWED, "HTTP method not supported for this endpoint");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ResponseData<Void>> handleNoResourceFoundException(Exception ex) {
        return buildError(HttpStatus.NOT_FOUND, "The requested endpoint does not exist");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResponseData<Void>> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("Invalid request !");
        return buildError(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ResponseData<Void>> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        return buildError(HttpStatus.BAD_REQUEST, "Malformed request body !");
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ResponseData<Void>> handleMissingRequestHeaderException(MissingRequestHeaderException ex) {
        return buildError(HttpStatus.BAD_REQUEST, "Required header '" + ex.getHeaderName() + "' is missing !");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ResponseData<Void>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex) {
        return buildError(HttpStatus.BAD_REQUEST, "Invalid value for parameter '" + ex.getName() + "'");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ResponseData<Void>> handleAccessDeniedException(AccessDeniedException ex) {
        return buildError(HttpStatus.FORBIDDEN, "You do not have permission to perform this action !");
    }

    private ResponseEntity<ResponseData<Void>> buildError(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ResponseData.error(message));
    }
}
