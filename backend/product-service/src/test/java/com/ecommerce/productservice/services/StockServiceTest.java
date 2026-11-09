package com.ecommerce.productservice.services;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ecommerce.productservice.dtos.request.StockChangeRequest.StockItem;
import com.ecommerce.productservice.exceptions.custom.InsufficientStockException;
import com.ecommerce.productservice.exceptions.custom.ResourceNotFoundException;
import com.ecommerce.productservice.repositories.ProductRepository;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock
    ProductRepository productRepository;

    @InjectMocks
    StockService stockService;

    @Test
    void reserve_decreasesEveryProduct() {
        when(productRepository.decreaseQuantityIfEnough("p1", 5, -5)).thenReturn(1L);
        when(productRepository.decreaseQuantityIfEnough("p2", 1, -1)).thenReturn(1L);

        stockService.reserve(List.of(new StockItem("p1", 5), new StockItem("p2", 1)));

        verify(productRepository).decreaseQuantityIfEnough("p1", 5, -5);
        verify(productRepository).decreaseQuantityIfEnough("p2", 1, -1);
        verify(productRepository, never()).increaseQuantity(anyString(), anyInt());
    }

    @Test
    void reserve_notEnoughOfSecondProduct_givesFirstOneBack() {
        when(productRepository.decreaseQuantityIfEnough("p1", 5, -5)).thenReturn(1L);
        when(productRepository.decreaseQuantityIfEnough("p2", 3, -3)).thenReturn(0L);
        when(productRepository.existsById("p2")).thenReturn(true);

        List<StockItem> items = List.of(new StockItem("p1", 5), new StockItem("p2", 3));

        assertThatThrownBy(() -> stockService.reserve(items))
                .isInstanceOf(InsufficientStockException.class);

        verify(productRepository).increaseQuantity("p1", 5);
    }

    @Test
    void reserve_unknownProduct_throwsNotFound() {
        when(productRepository.decreaseQuantityIfEnough("missing", 1, -1)).thenReturn(0L);
        when(productRepository.existsById("missing")).thenReturn(false);

        List<StockItem> items = List.of(new StockItem("missing", 1));

        assertThatThrownBy(() -> stockService.reserve(items))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void release_increasesStock_andSkipsDeletedProducts() {
        when(productRepository.increaseQuantity("p1", 5)).thenReturn(1L);
        when(productRepository.increaseQuantity("deleted", 2)).thenReturn(0L);

        List<StockItem> items = List.of(new StockItem("p1", 5), new StockItem("deleted", 2));

        assertThatCode(() -> stockService.release(items))
                .doesNotThrowAnyException();

        verify(productRepository).increaseQuantity("p1", 5);
        verify(productRepository).increaseQuantity("deleted", 2);
    }
}
