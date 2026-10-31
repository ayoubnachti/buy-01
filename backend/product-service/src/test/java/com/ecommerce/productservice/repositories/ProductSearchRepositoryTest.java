package com.ecommerce.productservice.repositories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.regex.Pattern;

import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import com.ecommerce.productservice.models.Product;

@ExtendWith(MockitoExtension.class)
class ProductSearchRepositoryTest {
  @Mock
  private MongoTemplate mongoTemplate;

  private ProductSearchRepository productSearchRepository;

  @BeforeEach
  void setUp() {
    productSearchRepository = new ProductSearchRepository(mongoTemplate);
  }

  @Test
  void emptyQueryReturnsAllProductsNewestFirst() {
    when(mongoTemplate.find(any(Query.class), eq(Product.class))).thenReturn(List.of());

    productSearchRepository.search("  ");

    ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
    verify(mongoTemplate).find(query.capture(), eq(Product.class));
    assertThat(query.getValue().getSortObject()).containsEntry("createdAt", -1);
    assertThat(query.getValue().getQueryObject()).isEmpty();
  }

  @Test
  void searchUsesTextAndAnEscapedCaseInsensitivePrefixFallback() {
    Product older = product("older", Instant.parse("2026-01-01T00:00:00Z"));
    Product newer = product("newer", Instant.parse("2026-02-01T00:00:00Z"));
    when(mongoTemplate.find(any(Query.class), eq(Product.class)))
        .thenReturn(List.of(older), List.of(older, newer));

    List<Product> results = productSearchRepository.search("TABLE");

    ArgumentCaptor<Query> queries = ArgumentCaptor.forClass(Query.class);
    verify(mongoTemplate, org.mockito.Mockito.times(2)).find(queries.capture(), eq(Product.class));
    assertThat(queries.getAllValues().get(0).getQueryObject())
        .containsKey("$text");
    assertThat((Document) queries.getAllValues().get(0).getQueryObject().get("$text"))
        .containsEntry("$search", "TABLE");

    Document prefixQuery = queries.getAllValues().get(1).getQueryObject();
    @SuppressWarnings("unchecked")
    List<Document> alternatives = (List<Document>) prefixQuery.get("$or");
    assertThat(alternatives).hasSize(2);
    Pattern namePattern = alternatives.get(0).get("name", Pattern.class);
    assertThat(namePattern.pattern()).isEqualTo("^\\QTABLE\\E");
    assertThat(namePattern.flags()).isEqualTo(Pattern.CASE_INSENSITIVE);
    assertThat(results).extracting(Product::getId).containsExactly("newer", "older");
  }

  @Test
  void textSearchReturnsProductsMatchingTheName() {
    Product product = Product.builder().id("name-match").name("Wireless keyboard").build();
    when(mongoTemplate.find(any(Query.class), eq(Product.class)))
        .thenReturn(List.of(product), List.of());

    List<Product> results = productSearchRepository.search("keyboard");

    assertThat(results).containsExactly(product);
    ArgumentCaptor<Query> textQuery = ArgumentCaptor.forClass(Query.class);
    verify(mongoTemplate, org.mockito.Mockito.times(2)).find(textQuery.capture(), eq(Product.class));
    assertThat((Document) textQuery.getAllValues().get(0).getQueryObject().get("$text"))
        .containsEntry("$search", "keyboard");
  }

  @Test
  void textSearchReturnsProductsMatchingTheDescription() {
    Product product = Product.builder()
        .id("description-match")
        .description("A quiet mechanical keyboard")
        .build();
    when(mongoTemplate.find(any(Query.class), eq(Product.class)))
        .thenReturn(List.of(product), List.of());

    List<Product> results = productSearchRepository.search("mechanical");

    assertThat(results).containsExactly(product);
    ArgumentCaptor<Query> textQuery = ArgumentCaptor.forClass(Query.class);
    verify(mongoTemplate, org.mockito.Mockito.times(2)).find(textQuery.capture(), eq(Product.class));
    assertThat((Document) textQuery.getAllValues().get(0).getQueryObject().get("$text"))
        .containsEntry("$search", "mechanical");
  }

  @Test
  void searchReturnsNoResultsWhenNeitherQueryMatches() {
    when(mongoTemplate.find(any(Query.class), eq(Product.class))).thenReturn(List.of());

    assertThat(productSearchRepository.search("nonexistent")).isEmpty();
    verify(mongoTemplate, org.mockito.Mockito.times(2)).find(any(Query.class), eq(Product.class));
  }

  @Test
  void regexMetacharactersAreTreatedAsLiteralPrefix() {
    when(mongoTemplate.find(any(Query.class), eq(Product.class))).thenReturn(List.of());

    productSearchRepository.search(".*(");

    ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
    verify(mongoTemplate).find(query.capture(), eq(Product.class));
    @SuppressWarnings("unchecked")
    List<Document> alternatives = (List<Document>) query.getValue().getQueryObject().get("$or");
    assertThat(alternatives.get(0).get("name", Pattern.class).pattern())
        .isEqualTo("^\\Q.*(\\E");
  }

  private Product product(String id, Instant createdAt) {
    return Product.builder().id(id).createdAt(createdAt).build();
  }
}
