package com.ecommerce.productservice.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.TextIndexDefinition;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import com.ecommerce.productservice.models.Product;
import com.ecommerce.productservice.models.ProductCategory;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ProductSearchIndexInitializer implements ApplicationRunner {
  private static final String CATEGORY_FIELD = "category";
  private static final String PRICE_FIELD = "price";
  private static final String CREATED_AT_FIELD = "createdAt";

  private final MongoTemplate mongoTemplate;

  @Override
  public void run(ApplicationArguments args) {
    TextIndexDefinition textIndex = TextIndexDefinition.builder()
        .onField("name", 10f)
        .onField("description", 1f)
        .named("product_name_description_text")
        .build();

    mongoTemplate.indexOps(Product.class).createIndex(textIndex);
    mongoTemplate.indexOps(Product.class).createIndex(new Index()
        .on(CATEGORY_FIELD, Sort.Direction.ASC)
        .on(PRICE_FIELD, Sort.Direction.ASC)
        .named("product_category_price"));
    mongoTemplate.indexOps(Product.class).createIndex(new Index()
        .on(CREATED_AT_FIELD, Sort.Direction.DESC)
        .named("product_created_at"));

    Query productsWithoutCategory = new Query(new Criteria().orOperator(
        Criteria.where(CATEGORY_FIELD).exists(false),
        Criteria.where(CATEGORY_FIELD).is(null)));
    mongoTemplate.updateMulti(
        productsWithoutCategory,
        Update.update(CATEGORY_FIELD, ProductCategory.OTHER),
        Product.class);
  }
}
