package com.ecommerce.productservice.repositories;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.stereotype.Repository;

import com.ecommerce.productservice.models.Product;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ProductSearchRepository {
  private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt");

  private final MongoTemplate mongoTemplate;

  public List<Product> search(String query) {
    String normalizedQuery = query == null ? "" : query.trim();
    if (normalizedQuery.isEmpty()) {
      return mongoTemplate.find(new Query().with(NEWEST_FIRST), Product.class);
    }

    Map<String, Product> matchesById = new LinkedHashMap<>();
    List<String> words = List.of(normalizedQuery.split("[^\\p{L}\\p{N}]+")).stream()
        .filter(word -> !word.isEmpty())
        .toList();

    if (!words.isEmpty()) {
      Query textQuery = new Query(TextCriteria.forDefaultLanguage()
          .matchingAny(words.toArray(String[]::new))).with(NEWEST_FIRST);
      addMatches(matchesById, mongoTemplate.find(textQuery, Product.class));
    }

    String prefixPattern = "^" + Pattern.quote(normalizedQuery);
    Query prefixQuery = new Query(new Criteria().orOperator(
        Criteria.where("name").regex(prefixPattern, "i"),
        Criteria.where("description").regex(prefixPattern, "i")))
        .with(NEWEST_FIRST);
    addMatches(matchesById, mongoTemplate.find(prefixQuery, Product.class));

    List<Product> matches = new ArrayList<>(matchesById.values());
    matches.sort(Comparator.comparing(Product::getCreatedAt,
        Comparator.nullsLast(Comparator.reverseOrder())));
    return matches;
  }

  private void addMatches(Map<String, Product> matchesById, List<Product> matches) {
    for (Product product : matches) {
      matchesById.put(product.getId(), product);
    }
  }
}
