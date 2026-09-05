package com.ecommerce.productservice;

import java.util.List;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.ecommerce.productservice.client.UserClient;
import com.ecommerce.productservice.dtos.ProductResponse;
import com.ecommerce.productservice.dtos.UserResponse;

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

  public ProductResponse getProductById(String id){
    Product product = productRepository.findById(id).orElse(null);
    return  ProductResponse.from(product);
  }

  public UserResponse getProductSeller(String id){
    ProductResponse product = getProductById(id);
    return userClient.getSeller(product.userId());
  }

  @KafkaListener(topics = "user-events", groupId = "product-service")
  public void deleteProductByUserId(String id ) { 
    productRepository.deleteByUserId(id);
  }
}
