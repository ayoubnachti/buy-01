package com.ecommerce.productservice.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ecommerce.productservice.Product;
import com.ecommerce.productservice.ProductRepository;

@Configuration
public class DataSeeder {

  @Bean
  CommandLineRunner seedProducts(ProductRepository repository) {
    return args -> {
      if (repository.count() == 0) {
        repository.save(Product.builder()
            .id("1")
            .name("product-1")
            .description("product 1 description")
            .price(10.)
            .quantity(100)
            .userId("1")
            .build());
        repository.save(Product.builder()
            .id("2")
            .name("product-2")
            .description("product 2 description")
            .price(12.5)
            .quantity(150)
            .userId("1")
            .build());
      }
    };
  }
}