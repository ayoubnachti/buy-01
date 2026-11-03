package com.ecommerce.cartservice.clients;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ecommerce.cartservice.dtos.response.ProductDto;
import com.ecommerce.cartservice.dtos.response.ResponseData;
import com.ecommerce.cartservice.exceptions.custom.ProductServiceUnavailableException;

import feign.FeignException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceGatewayTest {

    @Mock
    private ProductServiceClient client;

    @InjectMocks
    private ProductServiceGateway gateway;

    private static ProductDto product(String id) {
        return new ProductDto(id, "Product " + id, "desc", new BigDecimal("10.00"), 5,
                "ELECTRONICS", "seller-1", List.of());
    }

    private static ResponseData<ProductDto> found(String id) {
        return ResponseData.success("ok", product(id));
    }

    @Test
    void getProduct_existing_returnsIt() {
        when(client.getProduct("p1")).thenReturn(found("p1"));

        assertThat(gateway.getProduct("p1").id()).isEqualTo("p1");
    }

    @Test
    void getProduct_notFound_returnsNull_notAnOutage() {
        when(client.getProduct("gone")).thenThrow(mock(FeignException.NotFound.class));

        assertThat(gateway.getProduct("gone")).isNull();
    }

    @Test
    void getProducts_returnsFoundOnes_andSkipsMissing() {
        when(client.getProduct("p1")).thenReturn(found("p1"));
        when(client.getProduct("gone")).thenThrow(mock(FeignException.NotFound.class));

        Map<String, ProductDto> result = gateway.getProducts(List.of("p1", "gone"));

        assertThat(result).containsOnlyKeys("p1");
    }

    @Test
    void getProducts_duplicateIds_callProductServiceOncePerId() {
        when(client.getProduct("p1")).thenReturn(found("p1"));

        gateway.getProducts(List.of("p1", "p1", "p1"));

        verify(client, times(1)).getProduct("p1");
    }

    @Test
    void getProductFallback_throwsUnavailable() {
        assertThatThrownBy(() -> gateway.getProductFallback("p1", new RuntimeException("down")))
                .isInstanceOf(ProductServiceUnavailableException.class);
    }

    @Test
    void getProductsFallback_throwsUnavailable() {
        assertThatThrownBy(() -> gateway.getProductsFallback(List.of("p1"), new RuntimeException("down")))
                .isInstanceOf(ProductServiceUnavailableException.class);
    }
}