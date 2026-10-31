package com.ecommerce.productservice.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.ecommerce.productservice.dtos.request.ProductSearchRequest;
import com.ecommerce.productservice.dtos.response.ProductSearchFacetsResponse;
import com.ecommerce.productservice.exceptions.custom.InvalidSearchQueryException;
import com.ecommerce.productservice.models.ProductCategory;
import com.ecommerce.productservice.services.ProductService;

class ProductSearchControllerTest {
  private final ProductService productService = mock(ProductService.class);
  private final ProductController productController = new ProductController(productService);

  @Test
  void searchUsesDefaultsAndPassesAllFiltersToService() {
    productController.searchProducts("", null, null, null, "relevance", 0, 12);

    ArgumentCaptor<ProductSearchRequest> request = ArgumentCaptor.forClass(ProductSearchRequest.class);
    verify(productService).searchProducts(request.capture());
    assertThat(request.getValue())
        .isEqualTo(new ProductSearchRequest("", null, null, null, "relevance", 0, 12));
  }

  @Test
  void searchPassesCategoryAndPriceFiltersToService() {
    List<ProductCategory> categories = List.of(ProductCategory.BOOKS, ProductCategory.HOME);
    BigDecimal minPrice = new BigDecimal("10.00");
    BigDecimal maxPrice = new BigDecimal("50.00");

    productController.searchProducts(
        "desk", categories, minPrice, maxPrice, "price_asc", 1, 25);

    verify(productService).searchProducts(new ProductSearchRequest(
        "desk", categories, minPrice, maxPrice, "price_asc", 1, 25));
  }

  @Test
  void facetsEndpointReturnsReadOnlyServiceResult() {
    ProductSearchFacetsResponse facets = new ProductSearchFacetsResponse(List.of(), null, null);
    when(productService.getSearchFacets()).thenReturn(facets);

    assertThat(productController.getSearchFacets().getData()).isSameAs(facets);
    verify(productService).getSearchFacets();
  }

  @Test
  void searchAcceptsQueryAtMaximumLengthAndMaximumPageSize() {
    String query = "x".repeat(100);

    productController.searchProducts(query, null, null, null, "newest", 0, 50);

    verify(productService).searchProducts(new ProductSearchRequest(
        query, null, null, null, "newest", 0, 50));
  }

  @Test
  void searchRejectsQueryLongerThanOneHundredCharacters() {
    String query = "x".repeat(101);

    assertInvalid(query, null, null, null, "relevance", 0, 12,
        "Search query must be at most 100 characters");
  }

  @Test
  void searchRejectsNegativePrices() {
    assertInvalid("", null, new BigDecimal("-0.01"), null, "relevance", 0, 12,
        "Prices must not be negative");
    assertInvalid("", null, null, new BigDecimal("-0.01"), "relevance", 0, 12,
        "Prices must not be negative");
  }

  @Test
  void searchRejectsMinimumPriceGreaterThanMaximumPrice() {
    assertInvalid("", null, new BigDecimal("20"), new BigDecimal("10"), "relevance", 0, 12,
        "minPrice must be less than or equal to maxPrice");
  }

  @Test
  void searchRejectsUnsupportedSortValues() {
    assertInvalid("", null, null, null, "popular", 0, 12,
        "sort must be one of: relevance, price_asc, price_desc, newest");
  }

  @Test
  void searchRejectsNegativePages() {
    assertInvalid("", null, null, null, "relevance", -1, 12, "page must not be negative");
  }

  @Test
  void searchRejectsPageSizesOutsideOneThroughFifty() {
    assertInvalid("", null, null, null, "relevance", 0, 0, "size must be between 1 and 50");
    assertInvalid("", null, null, null, "relevance", 0, 51, "size must be between 1 and 50");
  }

  private void assertInvalid(
      String query,
      List<ProductCategory> categories,
      BigDecimal minPrice,
      BigDecimal maxPrice,
      String sort,
      int page,
      int size,
      String message) {
    assertThatThrownBy(() ->
        productController.searchProducts(query, categories, minPrice, maxPrice, sort, page, size))
        .isInstanceOf(InvalidSearchQueryException.class)
        .hasMessage(message);
  }
}
