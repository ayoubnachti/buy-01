package com.ecommerce.cartservice.dtos.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.ecommerce.cartservice.models.Cart;

import static org.assertj.core.api.Assertions.assertThat;

class CartResponseTest {

  private static final Instant NOW = Instant.parse("2026-11-03T10:00:00Z");

  @Test
  void availableLines_haveLiveDataAndCountInSubtotal() {
    Cart cart = new Cart("u1");
    cart.addItem("p1", 2, NOW);
    cart.addItem("p2", 1, NOW);

    CartResponse r = CartResponse.from(cart, Map.of("p1", product("p1", 10), "p2", product("p2", 5)));

    assertThat(r.items()).allMatch(CartResponse.Item::available);
    assertThat(r.items().get(0).name()).isEqualTo("Product p1");
    assertThat(r.items().get(0).lineTotal()).isEqualByComparingTo("20.00");
    assertThat(r.items().get(0).imageUrl()).isEqualTo("https://img/p1.jpg");
    assertThat(r.subtotal()).isEqualByComparingTo("30.00");
  }

  @Test
  void deletedProduct_isUnavailable_andNotInSubtotal() {
    Cart cart = new Cart("u1");
    cart.addItem("p1", 2, NOW);
    cart.addItem("gone", 1, NOW);

    CartResponse r = CartResponse.from(cart, Map.of("p1", product("p1", 10))); // "gone" missing

    CartResponse.Item gone = r.items().get(1);
    assertThat(gone.available()).isFalse();
    assertThat(gone.price()).isNull();
    assertThat(gone.lineTotal()).isNull();
    assertThat(r.subtotal()).isEqualByComparingTo("20.00");
  }

  @Test
  void stockBelowQuantity_isUnavailable_andNotInSubtotal() {
    Cart cart = new Cart("u1");
    cart.addItem("p1", 5, NOW);

    CartResponse r = CartResponse.from(cart, Map.of("p1", product("p1", 3)));

    assertThat(r.items().get(0).available()).isFalse();
    assertThat(r.subtotal()).isEqualByComparingTo("0");
  }

  @Test
  void productWithoutImages_hasNullImageUrl() {
    Cart cart = new Cart("u1");
    cart.addItem("p1", 1, NOW);
    ProductDto noImage = new ProductDto("p1", "X", "d", new BigDecimal("1.00"), 5, "C", "s", List.of());

    assertThat(CartResponse.from(cart, Map.of("p1", noImage)).items().get(0).imageUrl()).isNull();
  }

  @Test
  void emptyCart_hasZeroSubtotal() {
    assertThat(CartResponse.from(new Cart("u1"), Map.of()).subtotal()).isEqualByComparingTo("0");
  }

  private static ProductDto product(String id, int stock) {
    return new ProductDto(id, "Product " + id, "desc", new BigDecimal("10.00"), stock,
        "ELECTRONICS", "seller-1", List.of("https://img/" + id + ".jpg"));
  }
}
