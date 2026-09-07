package com.ecommerce.productservice.models;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Document(collection = "products")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class Product {
  @Id
  private String id;

  @NotBlank
  private String name;

  @NotBlank
  private String description;

  @Positive
  private BigDecimal price;

  @PositiveOrZero
  private Integer quantity;

  @NotBlank
  private String userId;

  @CreatedDate
  private Instant createdAt;

  @LastModifiedDate
  private Instant updatedAt;

  private List<String> imageUrls;
}
