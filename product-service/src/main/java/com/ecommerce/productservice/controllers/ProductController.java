package com.ecommerce.productservice.controllers;

import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.productservice.dtos.request.ProductRequest;
import com.ecommerce.productservice.dtos.response.*;
import com.ecommerce.productservice.services.ProductService;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

import java.util.List;

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
import org.springframework.web.bind.annotation.PutMapping;

@RestController
@RequestMapping("/products")
@AllArgsConstructor
public class ProductController {
  private final ProductService productService;

  @GetMapping()
  public List<ProductResponse> getAllProducts() {
    return this.productService.getAllProducts();
  }

  @GetMapping("/{id}")
  public ProductResponse getProduct(@PathVariable String id) {
    return productService.getProductById(id);
  }

  @PreAuthorize("hasRole('SELLER')")
  @PostMapping
  public ResponseEntity<ProductResponse> createProduct(
      @Valid @RequestBody ProductRequest request,
      @RequestHeader("X-User-Id") String userId) {
    ProductResponse product = productService.create(request, userId);
    return ResponseEntity.status(HttpStatus.CREATED).body(product);
  }

  @PreAuthorize("hasRole('SELLER')")
  @PutMapping("/{id}")
  public ResponseEntity<ProductResponse> updateProduct(
      @PathVariable String id,
      @Valid @RequestBody ProductRequest request,
      @RequestHeader("X-User-Id") String userId) {
    ProductResponse product = productService.updateProduct(request, id, userId);
    return ResponseEntity.ok(product);
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
