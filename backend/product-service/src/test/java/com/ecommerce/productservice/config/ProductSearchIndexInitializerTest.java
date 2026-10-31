package com.ecommerce.productservice.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexDefinition;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import com.ecommerce.productservice.models.Product;
import com.ecommerce.productservice.models.ProductCategory;

@ExtendWith(MockitoExtension.class)
class ProductSearchIndexInitializerTest {
  @Mock
  private MongoTemplate mongoTemplate;

  @Mock
  private IndexOperations indexOperations;

  private ProductSearchIndexInitializer initializer;

  @BeforeEach
  void setUp() {
    initializer = new ProductSearchIndexInitializer(mongoTemplate);
    when(mongoTemplate.indexOps(Product.class)).thenReturn(indexOperations);
  }

  @Test
  void startupEnsuresWeightedTextIndexForNameAndDescription() {
    initializer.run(mock(ApplicationArguments.class));

    ArgumentCaptor<IndexDefinition> definition = ArgumentCaptor.forClass(IndexDefinition.class);
    verify(indexOperations).createIndex(definition.capture());
    assertThat(definition.getValue().getIndexKeys())
        .containsEntry("name", "text")
        .containsEntry("description", "text");

    Document weights = (Document) definition.getValue().getIndexOptions().get("weights");
    assertThat(weights).containsEntry("name", 10f).doesNotContainKey("description");
    assertThat(definition.getValue().getIndexOptions())
        .containsEntry("name", "product_name_description_text");
  }

  @Test
  void startupBackfillsMissingAndNullCategoriesToOther() {
    initializer.run(mock(ApplicationArguments.class));

    ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
    ArgumentCaptor<Update> update = ArgumentCaptor.forClass(Update.class);
    verify(mongoTemplate).updateMulti(query.capture(), update.capture(), eq(Product.class));

    @SuppressWarnings("unchecked")
    java.util.List<Document> missingCategoryConditions =
        (java.util.List<Document>) query.getValue().getQueryObject().get("$or");
    assertThat(missingCategoryConditions).hasSize(2);
    assertThat(update.getValue().getUpdateObject())
        .containsEntry("$set", new Document("category", ProductCategory.OTHER));
  }
}
