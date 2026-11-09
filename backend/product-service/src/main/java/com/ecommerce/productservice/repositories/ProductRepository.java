package com.ecommerce.productservice.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;

import com.ecommerce.productservice.models.Product;

public interface ProductRepository extends MongoRepository<Product, String> {

  Page<Product> findByUserId(String userId, Pageable pageable);

  public String deleteByUserId(String id);

  /**
   * Lowers the stock by `quantity`, only if at least `quantity` are left.
   * The check and the change are one MongoDB operation, so two orders for the
   * last unit can't both succeed.
   * 
   * @return 1 if the stock was lowered, 0 if the product doesn't exist or has too
   *         few left
   */
  @Query("{ '_id': ?0, 'quantity': { '$gte': ?1 } }")
  @Update("{ '$inc': { 'quantity': ?2 } }")
  public long decreaseQuantityIfEnough(String productId, int quantity, int negativeQuantity);

  /** @return 1 if the product exists (stock raised), 0 if it doesn't */
  @Query("{ '_id': ?0 }")
  @Update("{ '$inc': { 'quantity': ?1 } }")
  public long increaseQuantity(String productId, int quantity);
}