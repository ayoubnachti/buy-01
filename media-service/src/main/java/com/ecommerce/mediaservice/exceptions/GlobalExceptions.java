package com.ecommerce.mediaservice.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.ecommerce.mediaservice.common.ResponseData;
import com.ecommerce.mediaservice.exceptions.Product.ProducIdNotFoundException;
import com.ecommerce.mediaservice.exceptions.media.CloudinaryUploadException;
import com.ecommerce.mediaservice.exceptions.media.ImageNotFoundException;
import com.ecommerce.mediaservice.exceptions.media.ImageNullOrEmptyException;
import com.ecommerce.mediaservice.exceptions.media.InvalidImageBodyException;
import com.ecommerce.mediaservice.exceptions.media.InvalidImageTypeException;
import com.ecommerce.mediaservice.exceptions.media.InvalidSizeLimitException;
import com.ecommerce.mediaservice.exceptions.media.MediaPersistenceException;

@RestControllerAdvice
public class GlobalExceptions {
    // need to call all the exceptions and also add the ones of method not allowed,
    // route not found...
    @ExceptionHandler(ImageNotFoundException.class)
    public ResponseEntity<ResponseData<Void>> handleImageNotFoundException(Exception ex) {
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(InvalidImageBodyException.class)
    public ResponseEntity<ResponseData<Void>> handleInvalidImageBodyException(Exception ex) {
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(ImageNullOrEmptyException.class)
    public ResponseEntity<ResponseData<Void>> handleImageNullOrEmptyException(Exception ex) {
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(InvalidImageTypeException.class)
    public ResponseEntity<ResponseData<Void>> handleInvalidImageTypeException(Exception ex) {
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(InvalidSizeLimitException.class)
    public ResponseEntity<ResponseData<Void>> handleInvalidSizeLimitException(Exception ex) {
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(ProducIdNotFoundException.class)
    public ResponseEntity<ResponseData<Void>> handleProducIdNotFoundException(Exception ex) {
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(CloudinaryUploadException.class)
    public ResponseEntity<ResponseData<Void>> handleCloudinaryUploadException(Exception ex) {
        return buildError(HttpStatus.BAD_GATEWAY, ex.getMessage());
    }

    @ExceptionHandler(MediaPersistenceException.class)
    public ResponseEntity<ResponseData<Void>> handleMediaPersistenceException(Exception ex) {
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    private ResponseEntity<ResponseData<Void>> buildError(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ResponseData.error(message));
    }
}
