package com.ecommerce.productservice.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.ecommerce.productservice.exceptions.custom.InvalidSearchQueryException;
import com.ecommerce.productservice.services.ProductService;

class ProductSearchControllerTest {
  private final ProductService productService = mock(ProductService.class);
  private final ProductController productController = new ProductController(productService);

  @Test
  void searchDefaultsToEmptyQuery() {
    assertThat(productController.searchProducts("").getData()).isEqualTo(List.of());
    verify(productService).searchProducts("");
  }

  @Test
  void searchAcceptsQueryAtMaximumLength() {
    String query = "x".repeat(100);

    productController.searchProducts(query);

    verify(productService).searchProducts(query);
  }

  @Test
  void searchRejectsQueryLongerThanOneHundredCharacters() {
    assertThatThrownBy(() -> productController.searchProducts("x".repeat(101)))
        .isInstanceOf(InvalidSearchQueryException.class)
        .hasMessage("Search query must be at most 100 characters");
  }
}
