package com.ecommerce.productservice.controllers;

import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.productservice.dtos.request.CreateProductRequest;
import com.ecommerce.productservice.dtos.response.*;
import com.ecommerce.productservice.models.Product;
import com.ecommerce.productservice.services.ProductService;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
@RequestMapping("/products")
@AllArgsConstructor
public class ProductController {
  private final ProductService productService;

  @GetMapping()
  public List<ProductResponse> getALlProducts() {
    return this.productService.getAllProducts();
  }

  @GetMapping("/{id}")
  public ProductResponse getProduct(@PathVariable String id) {
    return productService.getProductById(id);
  }

  @PreAuthorize("hasRole('SELLER')")
  @PostMapping
  public ResponseEntity<ProductResponse> createProduct(
      @Valid @RequestBody CreateProductRequest request,
      @RequestHeader("X-User-Id") String userId) {
    Product product = productService.create(request, userId);
    return ResponseEntity.status(HttpStatus.CREATED).body(ProductResponse.from(product));
  }

  @GetMapping("/{id}/seller")
  public UserResponse getMethodName(@PathVariable String id) {
    return productService.getProductSeller(id);
  }


  
}
