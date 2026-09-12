package com.ecommerce.productservice.dtos;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.ecommerce.productservice.dtos.request.ProductRequest;

class ProductRequestValidationTest {

  private static ValidatorFactory factory;
  private static Validator validator;

  @BeforeAll
  static void setUp() {
    factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @AfterAll
  static void tearDown() {
    factory.close();
  }

  @Test
  void validRequest_hasNoViolations() {
    ProductRequest request = new ProductRequest(
        "Keyboard", "Mechanical, brown switches", BigDecimal.TEN, 3, List.of("http://img/1.png"));

    assertThat(validator.validate(request)).isEmpty();
  }

  @Test
  void blankName_isRejected() {
    ProductRequest request = new ProductRequest("", "Desc", BigDecimal.TEN, 1, null);
    Set<ConstraintViolation<ProductRequest>> violations = validator.validate(request);
    assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("name"));
  }

  @Test
  void nonPositivePrice_isRejected() {
    ProductRequest request = new ProductRequest("Name", "Desc", BigDecimal.ZERO, 1, null);
    Set<ConstraintViolation<ProductRequest>> violations = validator.validate(request);
    assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("price"));
  }

  @Test
  void negativeQuantity_isRejected() {
    ProductRequest request = new ProductRequest("Name", "Desc", BigDecimal.TEN, -1, null);
    Set<ConstraintViolation<ProductRequest>> violations = validator.validate(request);
    assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("quantity"));
  }

  @Test
  void blankImageUrlInList_isRejected() {
    ProductRequest request = new ProductRequest(
        "Name", "Desc", BigDecimal.TEN, 1, List.of("http://img/1.png", "  "));
    Set<ConstraintViolation<ProductRequest>> violations = validator.validate(request);
    assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().startsWith("imageUrls"));
  }

  @Test
  void tooManyImageUrls_isRejected() {
    List<String> urls = java.util.stream.IntStream.range(0, 21)
        .mapToObj(i -> "http://img/" + i + ".png")
        .toList();
    ProductRequest request = new ProductRequest("Name", "Desc", BigDecimal.TEN, 1, urls);
    Set<ConstraintViolation<ProductRequest>> violations = validator.validate(request);
    assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("imageUrls"));
  }

  @Test
  void nullImageUrls_isAccepted() {
    ProductRequest request = new ProductRequest("Name", "Desc", BigDecimal.TEN, 1, null);
    assertThat(validator.validate(request)).isEmpty();
  }
}