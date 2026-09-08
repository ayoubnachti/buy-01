package com.ecommerce.mediaservice.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.http.HttpMethod;

import com.ecommerce.mediaservice.dtos.response.ErrorResponse;
import com.ecommerce.mediaservice.exceptions.GlobalExceptionHandler;
import com.ecommerce.mediaservice.exceptions.custom.ResourceNotFoundException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleResourceNotFound_returns404WithMessage() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Media not found");

        ResponseEntity<ErrorResponse> response = handler.handleResourceNotFound(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Media not found");
    }

    @Test
    void handleNoResourceFound_returns404WithGenericMessage() {
        NoResourceFoundException ex = new NoResourceFoundException(HttpMethod.GET, "/foo", "/foo");

        ResponseEntity<ErrorResponse> response = handler.handleNoResourceFound(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("The requested endpoint does not exist");
    }

    @Test
    void handleAccessDenied_returns403WithGenericMessage() {
        AccessDeniedException ex = new AccessDeniedException("denied");

        ResponseEntity<ErrorResponse> response = handler.handleAccessDenied(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message())
                .isEqualTo("You do not have permission to perform this action");
    }

    @Test
    void handleGeneric_returns500WithGenericMessage() {
        Exception ex = new RuntimeException("something exploded");

        ResponseEntity<ErrorResponse> response = handler.handleGeneric(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("An unexpected error occurred");
    }
}