package com.ecommerce.productservice.repositories;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.bson.Document;
import org.bson.types.Decimal128;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.data.mongodb.core.query.TextQuery;
import org.springframework.stereotype.Repository;

import com.ecommerce.productservice.dtos.request.ProductSearchRequest;
import com.ecommerce.productservice.dtos.response.ProductSearchFacetsResponse;
import com.ecommerce.productservice.dtos.response.ProductSearchFacetsResponse.CategoryCount;
import com.ecommerce.productservice.models.Product;
import com.ecommerce.productservice.models.ProductCategory;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ProductSearchRepository {
  private static final String CATEGORY_FIELD = "category";
  private static final String PRICE_FIELD = "price";
  private static final String CREATED_AT_FIELD = "createdAt";

  private final MongoTemplate mongoTemplate;

  public Page<Product> search(ProductSearchRequest request) {
    Query countQuery = buildQuery(request, false);
    long totalElements = mongoTemplate.count(countQuery, Product.class);

    boolean usePrefixFallback = totalElements == 0 && hasTextTokens(request.query());
    if (usePrefixFallback) {
      countQuery = buildPrefixQuery(request, false);
      totalElements = mongoTemplate.count(countQuery, Product.class);
    }

    Query pageQuery = usePrefixFallback
        ? buildPrefixQuery(request, true)
        : buildQuery(request, true);
    List<Product> content = mongoTemplate.find(pageQuery, Product.class);
    return new PageImpl<>(content, PageRequest.of(request.page(), request.size()), totalElements);
  }

  Query buildQuery(ProductSearchRequest request, boolean paged) {
    String normalizedQuery = request.query() == null ? "" : request.query().trim();
    List<String> words = List.of(normalizedQuery.split("[^\\p{L}\\p{N}]+")).stream()
        .filter(word -> !word.isEmpty())
        .toList();

    Query query;
    if (!words.isEmpty()) {
      query = TextQuery.queryText(TextCriteria.forDefaultLanguage()
          .matchingAny(words.toArray(String[]::new)));
    } else {
      query = new Query();
      if (!normalizedQuery.isEmpty()) {
        query.addCriteria(prefixCriteria(normalizedQuery));
      }
    }

    addFilters(query, request);

    if (request.sort().equals("relevance") && query instanceof TextQuery textQuery) {
      textQuery.sortByScore();
    } else {
      query.with(sortFor(request.sort()));
    }
    if (paged) {
      query.skip((long) request.page() * request.size()).limit(request.size());
    }
    return query;
  }

  private Query buildPrefixQuery(ProductSearchRequest request, boolean paged) {
    Query query = new Query(prefixCriteria(request.query().trim()));
    addFilters(query, request);
    query.with(sortFor(request.sort()));
    if (paged) {
      query.skip((long) request.page() * request.size()).limit(request.size());
    }
    return query;
  }

  private Criteria prefixCriteria(String query) {
    String prefixPattern = "^" + Pattern.quote(query);
    return new Criteria().orOperator(
        Criteria.where("name").regex(prefixPattern, "i"),
        Criteria.where("description").regex(prefixPattern, "i"));
  }

  private void addFilters(Query query, ProductSearchRequest request) {
    List<Criteria> filters = new ArrayList<>();
    if (request.categories() != null && !request.categories().isEmpty()) {
      filters.add(Criteria.where(CATEGORY_FIELD).in(request.categories()));
    }
    if (request.minPrice() != null || request.maxPrice() != null) {
      Criteria priceCriteria = Criteria.where(PRICE_FIELD);
      if (request.minPrice() != null) {
        priceCriteria.gte(request.minPrice());
      }
      if (request.maxPrice() != null) {
        priceCriteria.lte(request.maxPrice());
      }
      filters.add(priceCriteria);
    }
    if (!filters.isEmpty()) {
      query.addCriteria(new Criteria().andOperator(filters));
    }
  }

  public ProductSearchFacetsResponse getFacets() {
    Aggregation categoriesAggregation = Aggregation.newAggregation(
        Aggregation.match(Criteria.where(CATEGORY_FIELD).exists(true).ne(null)),
        Aggregation.group(CATEGORY_FIELD).count().as("count"),
        Aggregation.sort(Sort.by(Sort.Direction.ASC, "_id")));
    AggregationResults<Document> categoryResults =
        mongoTemplate.aggregate(categoriesAggregation, Product.class, Document.class);
    List<CategoryCount> categories = categoryResults.getMappedResults().stream()
        .map(result -> new CategoryCount(
            ProductCategory.valueOf(result.getString("_id")),
            ((Number) result.get("count")).longValue()))
        .toList();

    Aggregation priceAggregation = Aggregation.newAggregation(
        Aggregation.group()
            .min(PRICE_FIELD).as("minPrice")
            .max(PRICE_FIELD).as("maxPrice"));
    AggregationResults<Document> priceResults =
        mongoTemplate.aggregate(priceAggregation, Product.class, Document.class);
    Document priceRange = priceResults.getUniqueMappedResult();

    return new ProductSearchFacetsResponse(
        categories,
        priceRange == null ? null : toBigDecimal(priceRange.get("minPrice")),
        priceRange == null ? null : toBigDecimal(priceRange.get("maxPrice")));
  }

  private Sort sortFor(String sort) {
    return switch (sort) {
      case "price_asc" -> Sort.by(Sort.Order.asc(PRICE_FIELD), Sort.Order.desc(CREATED_AT_FIELD));
      case "price_desc" -> Sort.by(Sort.Order.desc(PRICE_FIELD), Sort.Order.desc(CREATED_AT_FIELD));
      case "newest", "relevance" -> Sort.by(Sort.Direction.DESC, CREATED_AT_FIELD);
      default -> throw new IllegalArgumentException("Unsupported product search sort: " + sort);
    };
  }

  private BigDecimal toBigDecimal(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Decimal128 decimal128) {
      return decimal128.bigDecimalValue();
    }
    if (value instanceof BigDecimal decimal) {
      return decimal;
    }
    if (value instanceof Number number) {
      return BigDecimal.valueOf(number.doubleValue());
    }
    throw new IllegalStateException("Unexpected MongoDB price value type: " + value.getClass().getName());
  }

  private boolean hasTextTokens(String query) {
    return query != null
        && List.of(query.trim().split("[^\\p{L}\\p{N}]+")).stream().anyMatch(word -> !word.isEmpty());
  }
}
