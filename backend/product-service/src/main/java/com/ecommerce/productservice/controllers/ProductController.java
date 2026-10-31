package com.ecommerce.productservice.controllers;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.productservice.dtos.request.ProductRequest;
import com.ecommerce.productservice.dtos.request.ProductSearchRequest;
import com.ecommerce.productservice.dtos.response.*;
import com.ecommerce.productservice.exceptions.custom.InvalidSearchQueryException;
import com.ecommerce.productservice.models.ProductCategory;
import com.ecommerce.productservice.services.ProductService;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PutMapping;

@RestController
@RequestMapping("/products")
@AllArgsConstructor
public class ProductController {
  private static final int MAX_SEARCH_QUERY_LENGTH = 100;
  private static final int MAX_SEARCH_SIZE = 50;
  private static final String DEFAULT_SEARCH_SORT = "relevance";
  private static final List<String> ALLOWED_SEARCH_SORTS =
      List.of(DEFAULT_SEARCH_SORT, "price_asc", "price_desc", "newest");

  private final ProductService productService;

  @GetMapping
  public ResponseData<ProductPageResponse> getAllProducts(
      @RequestParam(required = false) String owner,
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer limit) {
    ProductPageResponse result = productService.getProducts(owner, page, limit);
    return ResponseData.success("Products retrieved successfully", result);
  }

  @GetMapping("/search")
  public ResponseData<ProductSearchPageResponse> searchProducts(
      @RequestParam(defaultValue = "") String q,
      @RequestParam(required = false) List<ProductCategory> category,
      @RequestParam(required = false) BigDecimal minPrice,
      @RequestParam(required = false) BigDecimal maxPrice,
      @RequestParam(defaultValue = DEFAULT_SEARCH_SORT) String sort,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "12") int size) {
    if (q.length() > MAX_SEARCH_QUERY_LENGTH) {
      throw new InvalidSearchQueryException("Search query must be at most 100 characters");
    }
    if ((minPrice != null && minPrice.signum() < 0)
        || (maxPrice != null && maxPrice.signum() < 0)) {
      throw new InvalidSearchQueryException("Prices must not be negative");
    }
    if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
      throw new InvalidSearchQueryException("minPrice must be less than or equal to maxPrice");
    }
    if (!ALLOWED_SEARCH_SORTS.contains(sort)) {
      throw new InvalidSearchQueryException("sort must be one of: " + String.join(", ", ALLOWED_SEARCH_SORTS));
    }
    if (page < 0) {
      throw new InvalidSearchQueryException("page must not be negative");
    }
    if (size < 1 || size > MAX_SEARCH_SIZE) {
      throw new InvalidSearchQueryException("size must be between 1 and 50");
    }

    ProductSearchRequest request = new ProductSearchRequest(
        q, category, minPrice, maxPrice, sort, page, size);
    return ResponseData.success("Products retrieved successfully", productService.searchProducts(request));
  }

  @GetMapping("/facets")
  public ResponseData<ProductSearchFacetsResponse> getSearchFacets() {
    return ResponseData.success("Product search facets retrieved successfully", productService.getSearchFacets());
  }

  @GetMapping("/{id}")
  public ResponseData<ProductResponse> getProduct(@PathVariable String id) {
    return ResponseData.success("Product retrieved successfully", productService.getProductById(id));
  }

  @PreAuthorize("hasRole('SELLER')")
  @PostMapping
  public ResponseEntity<ResponseData<ProductResponse>> createProduct(
      @Valid @RequestBody ProductRequest request,
      @RequestHeader("X-User-Id") String userId) {
    ProductResponse product = productService.create(request, userId);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ResponseData.success("Product created successfully", product));
  }

  @PreAuthorize("hasRole('SELLER')")
  @PutMapping("/{id}")
  public ResponseEntity<ResponseData<ProductResponse>> updateProduct(
      @PathVariable String id,
      @Valid @RequestBody ProductRequest request,
      @RequestHeader("X-User-Id") String userId) {
    ProductResponse product = productService.updateProduct(request, id, userId);
    return ResponseEntity.ok(ResponseData.success("Product updated successfully", product));
  }

  @PreAuthorize("hasRole('SELLER')")
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteProduct(
      @PathVariable String id,
      @RequestHeader("X-User-Id") String userId) {
    productService.deleteProduct(id, userId);
    return ResponseEntity.noContent().build();
  }
}