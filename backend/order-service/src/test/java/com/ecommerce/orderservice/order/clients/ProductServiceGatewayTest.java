package com.ecommerce.orderservice.order.clients;

import com.ecommerce.orderservice.common.ResponseData;
import com.ecommerce.orderservice.exceptions.orderExceptions.OutOfStockException;
import com.ecommerce.orderservice.exceptions.orderExceptions.ProductNotFoundException;
import com.ecommerce.orderservice.exceptions.orderExceptions.ProductServiceUnavailableException;
import com.ecommerce.orderservice.order.dtos.request.StockChangeRequest;
import com.ecommerce.orderservice.order.dtos.request.StockChangeRequest.StockItem;
import com.ecommerce.orderservice.order.dtos.response.ProductDto;

import feign.FeignException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Plain unit test: no Spring, so the @CircuitBreaker annotations are not active here. */
@ExtendWith(MockitoExtension.class)
class ProductServiceGatewayTest {

    @Mock
    ProductServiceClient productServiceClient;

    @InjectMocks
    ProductServiceGateway gateway;

    private final Map<String, Integer> fiveOfP1 = Map.of("p1", 5);
    private final StockChangeRequest fiveOfP1Request = new StockChangeRequest(List.of(new StockItem("p1", 5)));

    // --- getProduct ---

    @Test
    void getProduct_returnsTheProduct() {
        ProductDto product = new ProductDto(
                "p1", "Mouse", "desc", new BigDecimal("9.99"), 10, "ELECTRONICS", "seller-1", List.of());
        when(productServiceClient.getProduct("p1")).thenReturn(ResponseData.success("ok", product));

        assertThat(gateway.getProduct("p1")).isEqualTo(product);
    }

    @Test
    void getProduct_unknownProduct_returnsNull() {
        when(productServiceClient.getProduct("p1")).thenThrow(mock(FeignException.NotFound.class));

        assertThat(gateway.getProduct("p1")).isNull();
    }

    @Test
    void getProductFallback_throwsServiceUnavailable() {
        RuntimeException cause = new RuntimeException("down");

        assertThatThrownBy(() -> gateway.getProductFallback("p1", cause))
                .isInstanceOf(ProductServiceUnavailableException.class);
    }

    // --- reserveStock / releaseStock ---

    @Test
    void reserveStock_sendsEveryProductAndQuantity() {
        gateway.reserveStock(fiveOfP1);

        verify(productServiceClient).reserveStock(fiveOfP1Request);
    }

    @Test
    void reserveStock_conflict_throwsOutOfStock() {
        doThrow(mock(FeignException.Conflict.class)).when(productServiceClient).reserveStock(any());

        assertThatThrownBy(() -> gateway.reserveStock(fiveOfP1))
                .isInstanceOf(OutOfStockException.class);
    }

    @Test
    void reserveStock_notFound_throwsProductNotFound() {
        doThrow(mock(FeignException.NotFound.class)).when(productServiceClient).reserveStock(any());

        assertThatThrownBy(() -> gateway.reserveStock(fiveOfP1))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void releaseStock_sendsEveryProductAndQuantity() {
        gateway.releaseStock(fiveOfP1);

        verify(productServiceClient).releaseStock(fiveOfP1Request);
    }

    // --- stockFallback ---

    @Test
    void stockFallback_outOfStock_isPassedThrough() {
        OutOfStockException outOfStock = new OutOfStockException("gone");

        assertThatThrownBy(() -> gateway.stockFallback(fiveOfP1, outOfStock))
                .isSameAs(outOfStock);
    }

    @Test
    void stockFallback_productNotFound_isPassedThrough() {
        ProductNotFoundException notFound = new ProductNotFoundException("missing");

        assertThatThrownBy(() -> gateway.stockFallback(fiveOfP1, notFound))
                .isSameAs(notFound);
    }

    @Test
    void stockFallback_otherError_throwsServiceUnavailable() {
        RuntimeException cause = new RuntimeException("down");

        assertThatThrownBy(() -> gateway.stockFallback(fiveOfP1, cause))
                .isInstanceOf(ProductServiceUnavailableException.class);
    }
}
