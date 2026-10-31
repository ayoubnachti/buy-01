package com.ecommerce.productservice.repositories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.bson.Document;
import org.bson.types.Decimal128;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Query;

import com.ecommerce.productservice.dtos.request.ProductSearchRequest;
import com.ecommerce.productservice.models.Product;
import com.ecommerce.productservice.models.ProductCategory;

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
  void filtersByCategoryAlone() {
    Query query = productSearchRepository.buildQuery(
        request(null, List.of(ProductCategory.BOOKS), null, null, "newest", 0, 12), false);

    assertThat(query.getQueryObject().toString())
        .contains("category")
        .contains("BOOKS")
        .contains("$in");
  }

  @Test
  void filtersByMinimumPriceAlone() {
    Query query = productSearchRepository.buildQuery(
        request(null, null, new BigDecimal("10.00"), null, "newest", 0, 12), false);

    assertThat(query.getQueryObject().toString())
        .contains("price")
        .contains("$gte")
        .contains("10.00");
  }

  @Test
  void filtersByMaximumPriceAlone() {
    Query query = productSearchRepository.buildQuery(
        request(null, null, null, new BigDecimal("50.00"), "newest", 0, 12), false);

    assertThat(query.getQueryObject().toString())
        .contains("price")
        .contains("$lte")
        .contains("50.00");
  }

  @Test
  void combinesTextCategoryAndBothPriceFilters() {
    Query query = productSearchRepository.buildQuery(
        request("desk", List.of(ProductCategory.HOME, ProductCategory.BOOKS),
            new BigDecimal("10"), new BigDecimal("50"), "relevance", 0, 12), false);

    assertThat(query.getQueryObject().toString())
        .contains("$text")
        .contains("$search")
        .contains("desk")
        .contains("category")
        .contains("HOME")
        .contains("BOOKS")
        .contains("price")
        .contains("$gte")
        .contains("$lte");
  }

  @Test
  void relevanceSortUsesTextScoreForTextSearch() {
    Query query = productSearchRepository.buildQuery(
        request("desk", null, null, null, "relevance", 0, 12), false);

    assertThat(query.getSortObject().toString()).contains("$meta", "textScore");
  }

  @Test
  void blankRelevanceSearchDefaultsToNewest() {
    Query query = productSearchRepository.buildQuery(
        request("", null, null, null, "relevance", 0, 12), false);

    assertThat(query.getSortObject()).containsEntry("createdAt", -1);
  }

  @Test
  void eachExplicitSortUsesExpectedFields() {
    assertThat(productSearchRepository.buildQuery(
        request("", null, null, null, "price_asc", 0, 12), false).getSortObject())
        .containsEntry("price", 1)
        .containsEntry("createdAt", -1);
    assertThat(productSearchRepository.buildQuery(
        request("", null, null, null, "price_desc", 0, 12), false).getSortObject())
        .containsEntry("price", -1)
        .containsEntry("createdAt", -1);
    assertThat(productSearchRepository.buildQuery(
        request("", null, null, null, "newest", 0, 12), false).getSortObject())
        .containsEntry("createdAt", -1);
  }

  @Test
  void pageTwoUsesExpectedOffset() {
    Query query = productSearchRepository.buildQuery(
        request("", null, null, null, "newest", 1, 12), true);

    assertThat(query.getSkip()).isEqualTo(12);
    assertThat(query.getLimit()).isEqualTo(12);
  }

  @Test
  void lastPageReturnsShortContentAndRetainsTotals() {
    Product product = Product.builder().id("last-item").build();
    when(mongoTemplate.count(any(Query.class), eq(Product.class))).thenReturn(25L);
    when(mongoTemplate.find(any(Query.class), eq(Product.class))).thenReturn(List.of(product));

    Page<Product> result = productSearchRepository.search(
        request("", null, null, null, "newest", 2, 12));

    assertThat(result.getContent()).containsExactly(product);
    assertThat(result.getNumber()).isEqualTo(2);
    assertThat(result.getSize()).isEqualTo(12);
    assertThat(result.getTotalElements()).isEqualTo(25);
    assertThat(result.getTotalPages()).isEqualTo(3);
  }

  @Test
  void outOfRangePageIsEmptyAndRetainsTotals() {
    when(mongoTemplate.count(any(Query.class), eq(Product.class))).thenReturn(25L);
    when(mongoTemplate.find(any(Query.class), eq(Product.class))).thenReturn(List.of());

    Page<Product> result = productSearchRepository.search(
        request("", null, null, null, "newest", 3, 12));

    assertThat(result.getContent()).isEmpty();
    assertThat(result.getNumber()).isEqualTo(3);
    assertThat(result.getTotalElements()).isEqualTo(25);
    assertThat(result.getTotalPages()).isEqualTo(3);
    ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
    verify(mongoTemplate).find(query.capture(), eq(Product.class));
    assertThat(query.getValue().getSkip()).isEqualTo(36);
  }

  @Test
  void facetsReturnCategoryCountsAndPriceRange() {
    when(mongoTemplate.aggregate(any(Aggregation.class), eq(Product.class), eq(Document.class)))
        .thenReturn(new AggregationResults<>(
            List.of(new Document("_id", "BOOKS").append("count", 4L)),
            new Document()))
        .thenReturn(new AggregationResults<>(
            List.of(new Document("minPrice", new Decimal128(new BigDecimal("5.25")))
                .append("maxPrice", new Decimal128(new BigDecimal("90.00")))),
            new Document()));

    var facets = productSearchRepository.getFacets();

    assertThat(facets.categories())
        .containsExactly(new com.ecommerce.productservice.dtos.response.ProductSearchFacetsResponse.CategoryCount(
            ProductCategory.BOOKS, 4));
    assertThat(facets.minPrice()).isEqualByComparingTo("5.25");
    assertThat(facets.maxPrice()).isEqualByComparingTo("90.00");
  }

  private ProductSearchRequest request(
      String query,
      List<ProductCategory> categories,
      BigDecimal minPrice,
      BigDecimal maxPrice,
      String sort,
      int page,
      int size) {
    return new ProductSearchRequest(query, categories, minPrice, maxPrice, sort, page, size);
  }
}
