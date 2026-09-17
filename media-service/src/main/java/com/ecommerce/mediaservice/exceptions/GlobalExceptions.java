package com.ecommerce.mediaservice.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.ecommerce.mediaservice.common.ResponseData;
import com.ecommerce.mediaservice.exceptions.Product.ProducIdNotFoundException;
import com.ecommerce.mediaservice.exceptions.media.ImageNotFoundException;
import com.ecommerce.mediaservice.exceptions.media.InvalidImageBodyException;
import com.ecommerce.mediaservice.exceptions.media.InvalidImageTypeException;
import com.ecommerce.mediaservice.exceptions.media.InvalidSizeLimitException;

@RestControllerAdvice
public class GlobalExceptions {
    // need to call all the exceptions and also add the ones of method not allowed,
    // route not found...
    @ExceptionHandler(ImageNotFoundException.class)
    public ResponseEntity<ResponseData<Void>> handleImageNotFoundException(Exception ex) {
        return buildError(HttpStatus.NOT_FOUND, "Image not found !");
    }

    @ExceptionHandler(InvalidImageBodyException.class)
    public ResponseEntity<ResponseData<Void>> handleInvalidImageBodyException(Exception ex) {
        return buildError(HttpStatus.BAD_REQUEST, "Invalid image body, the data of the body is not of images !");
    }

    @ExceptionHandler(InvalidImageTypeException.class)
    public ResponseEntity<ResponseData<Void>> handleInvalidImageTypeException(Exception ex) {
        return buildError(HttpStatus.BAD_REQUEST, "Invalid image type, we only allow images !");
    }

    @ExceptionHandler(InvalidSizeLimitException.class)
    public ResponseEntity<ResponseData<Void>> handleInvalidSizeLimitException(Exception ex) {
        return buildError(HttpStatus.BAD_REQUEST,
                "The image has more than 2MG, please use images with less than 2MG in the size !");
    }

    @ExceptionHandler(ProducIdNotFoundException.class)
    public ResponseEntity<ResponseData<Void>> handleProducIdNotFoundException(Exception ex) {
        return buildError(HttpStatus.NOT_FOUND,
                "The product ID, is not found !");
    }

    private ResponseEntity<ResponseData<Void>> buildError(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ResponseData.error(message));
    }
}
