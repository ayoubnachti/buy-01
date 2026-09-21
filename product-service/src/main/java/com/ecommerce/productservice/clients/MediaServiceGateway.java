package com.ecommerce.productservice.clients;

import java.util.List;

import org.springframework.stereotype.Component;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class MediaServiceGateway {
  private final MediaServiceClient mediaServiceClient;

  @CircuitBreaker(name = "mediaService", fallbackMethod = "getImagesFallback")
  public List<String> getImages(String productId) {
    MediaImagesResponse response = mediaServiceClient.getMedias(productId);
    return response.data() != null ? response.data() : List.of();
  }

  private List<String> getImagesFallback(String productId, Throwable t) {
    log.warn("Media Service unavailable, returning no images for product {}: {}", productId, t.getMessage());
    return List.of();
  }
} 
