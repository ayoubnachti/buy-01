package com.ecommerce.productservice.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.TextIndexDefinition;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import com.ecommerce.productservice.models.Product;
import com.ecommerce.productservice.models.ProductCategory;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ProductSearchIndexInitializer implements ApplicationRunner {
  private final MongoTemplate mongoTemplate;

  @Override
  public void run(ApplicationArguments args) {
    TextIndexDefinition textIndex = TextIndexDefinition.builder()
        .onField("name", 10f)
        .onField("description", 1f)
        .named("product_name_description_text")
        .build();

    mongoTemplate.indexOps(Product.class).ensureIndex(textIndex);

    Query productsWithoutCategory = new Query(new Criteria().orOperator(
        Criteria.where("category").exists(false),
        Criteria.where("category").is(null)));
    mongoTemplate.updateMulti(
        productsWithoutCategory,
        Update.update("category", ProductCategory.OTHER),
        Product.class);
  }
}
