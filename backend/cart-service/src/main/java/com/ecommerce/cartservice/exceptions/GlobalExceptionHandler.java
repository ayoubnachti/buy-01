package com.ecommerce.cartservice.exceptions;

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

import com.ecommerce.cartservice.common.ResponseData;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseData<Void>> handleUnexpectedException(Exception ex) {
        log.error("Unhandled exception", ex);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong. Please try again later.");
    }

    private ResponseEntity<ResponseData<Void>> buildError(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ResponseData.error(message));
    }
}