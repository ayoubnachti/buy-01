package com.ecommerce.productservice.services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.ecommerce.productservice.client.UserClient;
import com.ecommerce.productservice.dtos.request.CreateProductRequest;
import com.ecommerce.productservice.dtos.response.*;
import com.ecommerce.productservice.exceptions.custom.ForbiddenException;
import com.ecommerce.productservice.exceptions.custom.ResourceNotFoundException;
import com.ecommerce.productservice.models.Product;
import com.ecommerce.productservice.repositories.ProductRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductService {
  private final ProductRepository productRepository;
  private final UserClient userClient;

  public List<ProductResponse> getAllProducts() {
    return productRepository.findAll()
        .stream()
        .map(product -> ProductResponse.from(product))
        .toList();
  }

  public ProductResponse getProductById(String id) {
    Product product = productRepository.findById(id).orElseThrow(
        () -> new ResourceNotFoundException("Product", id));
    return ProductResponse.from(product);
  }

  public UserResponse getProductSeller(String id) {
    ProductResponse product = getProductById(id);
    return userClient.getSeller(product.userId());
  }

  public Product create(CreateProductRequest request, String sellerId) {

    Product product = Product.builder()
        .name(request.name())
        .description(request.description())
        .price(request.price())
        .quantity(request.quantity())
        .userId(sellerId)
        .imageUrls(new ArrayList<>())
        .build();
    return productRepository.save(product);
  }

  @KafkaListener(topics = "user-events", groupId = "product-service")
  public void deleteProductByUserId(String id) {
    productRepository.deleteByUserId(id);
  }
}
