package com.ecommerce.productservice.services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.ecommerce.productservice.dtos.request.ProductRequest;
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

  public List<ProductResponse> getAllProducts() {
    return productRepository.findAll()
        .stream()
        .map(product -> ProductResponse.from(product))
        .toList();
  }

  public ProductResponse getProductById(String id) {
    return ProductResponse.from(findProductById(id));
  }

  public ProductResponse create(ProductRequest request, String sellerId) {
    Product product = Product.builder()
        .name(request.name())
        .description(request.description())
        .price(request.price())
        .quantity(request.quantity())
        .userId(sellerId)
        .imageUrls(request.imageUrls() != null ? request.imageUrls() : new ArrayList<>())
        .build();

    return ProductResponse.from(productRepository.save(product));
  }

  public ProductResponse updateProduct(ProductRequest req, String id, String userId) {
    Product existingProduct = findProductById(id);

    if (!existingProduct.getUserId().equals(userId)) {
      throw new ForbiddenException("You do not own this product");
    }

    existingProduct.setName(req.name());
    existingProduct.setDescription(req.description());
    existingProduct.setPrice(req.price());
    existingProduct.setQuantity(req.quantity());
    existingProduct.setImageUrls(req.imageUrls());

    Product saved = productRepository.save(existingProduct);
    return ProductResponse.from(saved);
  }

  @KafkaListener(topics = "user-events", groupId = "product-service")
  public void deleteProductByUserId(String id) {
    productRepository.deleteByUserId(id);
  }

  private Product findProductById(String id) {
    return productRepository.findById(id).orElseThrow(
        () -> new ResourceNotFoundException("Product", id));
  }
}
