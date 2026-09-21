package com.ecommerce.productservice.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "media-service")
public interface MediaServiceClient {

  @GetMapping("/media/{productId}")
  MediaImagesResponse getMedias(@PathVariable("productId") String productId);
}