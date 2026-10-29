package com.ecommerce.cartservice.exceptions;

import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.ecommerce.cartservice.exceptions.custom.CartItemNotFoundException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void methodNotAllowed_returns405() {
        assertThat(handler.handleMethodNotAllowedException(new RuntimeException()).getStatusCode())
                .isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    }

    @Test
    void noResource_returns404() {
        assertThat(handler.handleNoResourceFoundException(new RuntimeException()).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void validationError_returns400_withFirstFieldMessage() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult binding = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(binding);
        when(binding.getFieldErrors())
                .thenReturn(List.of(new FieldError("item", "quantity", "Quantity must be at least 1")));

        var response = handler.handleMethodArgumentNotValidException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo("Quantity must be at least 1");
    }

    @Test
    void validationError_withoutFieldErrors_usesDefaultMessage() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult binding = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(binding);
        when(binding.getFieldErrors()).thenReturn(List.of());

        assertThat(handler.handleMethodArgumentNotValidException(ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void unreadableBody_returns400() {
        assertThat(handler.handleHttpMessageNotReadableException(mock(HttpMessageNotReadableException.class))
                .getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void missingHeader_returns400() {
        MissingRequestHeaderException ex = mock(MissingRequestHeaderException.class);
        when(ex.getHeaderName()).thenReturn("X-User-Id");
        assertThat(handler.handleMissingRequestHeaderException(ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void typeMismatch_returns400() {
        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
        when(ex.getName()).thenReturn("quantity");
        assertThat(handler.handleMethodArgumentTypeMismatchException(ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void accessDenied_returns403() {
        assertThat(handler.handleAccessDeniedException(new AccessDeniedException("no")).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void unexpectedError_returns500_withSafeMessage() {
        var response = handler.handleUnexpectedException(new IllegalStateException("db password is hunter2"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getMessage()).doesNotContain("hunter2");
    }

    @Test
    void cartItemNotFound_returns404() {
        assertThat(handler.handleCartItemNotFoundException(new CartItemNotFoundException("p1")).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void concurrentChange_returns409() {
        assertThat(handler.handleConcurrentCartChange(new OptimisticLockingFailureException("x")).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }
}