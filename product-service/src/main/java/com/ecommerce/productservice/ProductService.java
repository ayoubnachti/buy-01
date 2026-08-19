package com.ecommerce.productservice;

import java.util.List;

import org.springframework.stereotype.Service;

import com.ecommerce.productservice.dtos.ProductResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductService {
  private final ProductRepository productRepository; 

  public List<ProductResponse> getAllProducts() {
    return productRepository.findAll()
      .stream()
      .map(product -> ProductResponse.from(product))
      .toList();
  }

  public ProductResponse getProductById(String id){
    Product product = productRepository.findById(id).orElse(null);
    return  ProductResponse.from(product);
  }
}
