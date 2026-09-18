package com.ecommerce.mediaservice.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.ecommerce.mediaservice.common.ResponseData;
import com.ecommerce.mediaservice.exceptions.Product.ProducIdNotFoundException;
import com.ecommerce.mediaservice.exceptions.media.CloudinaryDeleteException;
import com.ecommerce.mediaservice.exceptions.media.CloudinaryUploadException;
import com.ecommerce.mediaservice.exceptions.media.ImageNotDeletedException;
import com.ecommerce.mediaservice.exceptions.media.ImageNotFoundException;
import com.ecommerce.mediaservice.exceptions.media.ImageNullOrEmptyException;
import com.ecommerce.mediaservice.exceptions.media.InvalidImageBodyException;
import com.ecommerce.mediaservice.exceptions.media.InvalidImageTypeException;
import com.ecommerce.mediaservice.exceptions.media.InvalidSizeLimitException;
import com.ecommerce.mediaservice.exceptions.media.MediaPersistenceException;
import com.ecommerce.mediaservice.exceptions.profile.ForbiddenToChangeProfileException;

@RestControllerAdvice
public class GlobalExceptions {
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

    @ExceptionHandler(CloudinaryDeleteException.class)
    public ResponseEntity<ResponseData<Void>> handleCloudinaryDeleteException(Exception ex) {
        return buildError(HttpStatus.BAD_GATEWAY, ex.getMessage());
    }

    @ExceptionHandler(MediaPersistenceException.class)
    public ResponseEntity<ResponseData<Void>> handleMediaPersistenceException(Exception ex) {
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    @ExceptionHandler(ForbiddenToChangeProfileException.class)
    public ResponseEntity<ResponseData<Void>> handleForbiddenToChangeProfileException(Exception ex) {
        return buildError(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(ImageNotDeletedException.class)
    public ResponseEntity<ResponseData<Void>> handleImageNotDeletedException(Exception ex) {
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ResponseData<Void>> handleMethodNotAllowedException(Exception ex) {
        return buildError(HttpStatus.METHOD_NOT_ALLOWED, "HTTP method not supported for this endpoint");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ResponseData<Void>> handleNoResourceFoundException(Exception ex) {
        return buildError(HttpStatus.NOT_FOUND, "The requested endpoint does not exist");
    }

    private ResponseEntity<ResponseData<Void>> buildError(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ResponseData.error(message));
    }
}
