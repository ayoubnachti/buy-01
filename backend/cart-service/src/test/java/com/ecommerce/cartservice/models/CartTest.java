package com.ecommerce.cartservice.models;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CartTest {

    private static final Instant T1 = Instant.parse("2026-10-27T10:00:00Z");
    private static final Instant T2 = Instant.parse("2026-10-27T11:00:00Z");

    @Test
    void addItem_newProduct_addsALine() {
        Cart cart = new Cart("user-1");

        cart.addItem("p1", 2, T1);

        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(2);
        assertThat(cart.getItems().get(0).getAddedAt()).isEqualTo(T1);
        assertThat(cart.getUpdatedAt()).isEqualTo(T1);
    }

    @Test
    void addItem_sameProductTwice_mergesQuantity_andKeepsFirstAddedAt() {
        Cart cart = new Cart("user-1");

        cart.addItem("p1", 2, T1);
        cart.addItem("p1", 3, T2);

        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(5);
        assertThat(cart.getItems().get(0).getAddedAt()).isEqualTo(T1);
        assertThat(cart.getUpdatedAt()).isEqualTo(T2);
    }

    @Test
    void updateQuantity_existingProduct_setsQuantity() {
        Cart cart = new Cart("user-1");
        cart.addItem("p1", 2, T1);

        boolean updated = cart.updateQuantity("p1", 7, T2);

        assertThat(updated).isTrue();
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(7);
        assertThat(cart.getUpdatedAt()).isEqualTo(T2);
    }

    @Test
    void updateQuantity_missingProduct_returnsFalse_andChangesNothing() {
        Cart cart = new Cart("user-1");
        cart.addItem("p1", 2, T1);

        boolean updated = cart.updateQuantity("other", 7, T2);

        assertThat(updated).isFalse();
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(2);
        assertThat(cart.getUpdatedAt()).isEqualTo(T1);
    }

    @Test
    void removeItem_existingProduct_removesIt() {
        Cart cart = new Cart("user-1");
        cart.addItem("p1", 2, T1);
        cart.addItem("p2", 1, T1);

        boolean removed = cart.removeItem("p1", T2);

        assertThat(removed).isTrue();
        assertThat(cart.getItems()).extracting(CartItem::getProductId).containsExactly("p2");
        assertThat(cart.getUpdatedAt()).isEqualTo(T2);
    }

    @Test
    void removeItem_missingProduct_returnsFalse_andChangesNothing() {
        Cart cart = new Cart("user-1");
        cart.addItem("p1", 2, T1);

        boolean removed = cart.removeItem("other", T2);

        assertThat(removed).isFalse();
        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getUpdatedAt()).isEqualTo(T1);
    }
}