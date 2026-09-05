package com.ecommerce.productservice;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProductRepository extends MongoRepository<Product, String> {

  public String deleteByUserId(String id);
}
