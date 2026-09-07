package com.ecommerce.userservice.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void shouldReturn400WhenValidationFails() {

        BindingResult bindingResult = Mockito.mock(BindingResult.class);

        FieldError fieldError = new FieldError(
                "registerRequest",
                "email",
                "must be a valid email"
        );

        Mockito.when(bindingResult.getFieldErrors())
                .thenReturn(List.of(fieldError));

        MethodArgumentNotValidException exception =
                new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<ErrorResponse> response =
                handler.handleValidationException(exception);

        assertEquals(
                HttpStatus.BAD_REQUEST.value(),
                response.getBody().status()
        );

        assertEquals(
                "email: must be a valid email",
                response.getBody().message()
        );
    }

    @Test
    void shouldReturn404WhenResourceIsNotFound() {

        ResourceNotFoundException exception =
                new ResourceNotFoundException("User not found");

        ResponseEntity<ErrorResponse> response =
                handler.handleResourceNotFoundException(exception);

        assertEquals(
                HttpStatus.NOT_FOUND.value(),
                response.getBody().status()
        );

        assertEquals(
                "User not found",
                response.getBody().message()
        );
    }

    @Test
    void shouldReturn401WhenCredentialsAreInvalid() {

        BadCredentialsException exception =
                new BadCredentialsException("Invalid email or password");

        ResponseEntity<ErrorResponse> response =
                handler.handleBadCredentialsException(exception);

        assertEquals(
                HttpStatus.UNAUTHORIZED.value(),
                response.getBody().status()
        );

        assertEquals(
                "Invalid email or password",
                response.getBody().message()
        );
    }

    @Test
    void shouldReturn400WhenIllegalArgumentIsThrown() {

        IllegalArgumentException exception =
                new IllegalArgumentException("Email already exists");

        ResponseEntity<ErrorResponse> response =
                handler.handleIllegalArgumentException(exception);

        assertEquals(
                HttpStatus.BAD_REQUEST.value(),
                response.getBody().status()
        );

        assertEquals(
                "Email already exists",
                response.getBody().message()
        );
    }

    @Test
    void shouldReturn500WhenUnexpectedExceptionOccurs() {

        Exception exception =
                new RuntimeException("Database failure");

        ResponseEntity<ErrorResponse> response =
                handler.handleGenericException(exception);

        assertEquals(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                response.getBody().status()
        );

        assertEquals(
                "An unexpected error occurred",
                response.getBody().message()
        );
    }
}
