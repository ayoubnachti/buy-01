package com.ecommerce.cartservice.services;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ecommerce.cartservice.models.Cart;
import com.ecommerce.cartservice.models.CartItem;
import com.ecommerce.cartservice.repositories.CartRepository;
import com.ecommerce.cartservice.exceptions.custom.CartItemNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-27T10:00:00Z");

    @Mock
    private CartRepository cartRepository;

    private CartService cartService;

    @BeforeEach
    void setUp() {
        cartService = new CartService(cartRepository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    /** A cart already in the database, with one item. */
    private Cart existingCart(String userId, String productId, int quantity) {
        Cart cart = new Cart(userId);
        cart.addItem(productId, quantity, NOW);
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        return cart;
    }

    private void saveReturnsItsArgument() {
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    // ---- getCart

    @Test
    void getCart_noCartInDatabase_returnsEmptyCart_withoutSaving() {
        when(cartRepository.findByUserId("user-1")).thenReturn(Optional.empty());

        Cart cart = cartService.getCart("user-1");

        assertThat(cart.getUserId()).isEqualTo("user-1");
        assertThat(cart.getItems()).isEmpty();
        verify(cartRepository, never()).save(any());
    }

    @Test
    void getCart_existingCart_returnsIt() {
        Cart stored = existingCart("user-1", "p1", 2);

        assertThat(cartService.getCart("user-1")).isSameAs(stored);
    }

    // ---- addItem

    @Test
    void addItem_noCartYet_createsCartWithTheItem() {
        when(cartRepository.findByUserId("user-1")).thenReturn(Optional.empty());
        saveReturnsItsArgument();

        cartService.addItem("user-1", "p1", 2);

        ArgumentCaptor<Cart> saved = ArgumentCaptor.forClass(Cart.class);
        verify(cartRepository).save(saved.capture());
        assertThat(saved.getValue().getUserId()).isEqualTo("user-1");
        assertThat(saved.getValue().getItems())
                .extracting(CartItem::getProductId, CartItem::getQuantity)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("p1", 2));
        assertThat(saved.getValue().getUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    void addItem_productAlreadyInCart_mergesQuantity() {
        existingCart("user-1", "p1", 2);
        saveReturnsItsArgument();

        Cart result = cartService.addItem("user-1", "p1", 3);

        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getQuantity()).isEqualTo(5);
    }

    // ---- updateQuantity

    @Test
    void updateQuantity_existingItem_savesNewQuantity() {
        existingCart("user-1", "p1", 2);
        saveReturnsItsArgument();

        Cart result = cartService.updateQuantity("user-1", "p1", 7);

        assertThat(result.getItems().get(0).getQuantity()).isEqualTo(7);
        verify(cartRepository).save(result);
    }

    @Test
    void updateQuantity_noCart_throwsNotFound() {
        when(cartRepository.findByUserId("user-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.updateQuantity("user-1", "p1", 7))
                .isInstanceOf(CartItemNotFoundException.class);
        verify(cartRepository, never()).save(any());
    }

    @Test
    void updateQuantity_productNotInCart_throwsNotFound_andDoesNotSave() {
        existingCart("user-1", "p1", 2);

        assertThatThrownBy(() -> cartService.updateQuantity("user-1", "other", 7))
                .isInstanceOf(CartItemNotFoundException.class);
        verify(cartRepository, never()).save(any());
    }

    // ---- removeItem

    @Test
    void removeItem_existingItem_removesAndSaves() {
        existingCart("user-1", "p1", 2);
        saveReturnsItsArgument();

        Cart result = cartService.removeItem("user-1", "p1");

        assertThat(result.getItems()).isEmpty();
        verify(cartRepository).save(result);
    }

    @Test
    void removeItem_noCart_throwsNotFound() {
        when(cartRepository.findByUserId("user-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.removeItem("user-1", "p1"))
                .isInstanceOf(CartItemNotFoundException.class);
    }

    @Test
    void removeItem_productNotInCart_throwsNotFound_andDoesNotSave() {
        existingCart("user-1", "p1", 2);

        assertThatThrownBy(() -> cartService.removeItem("user-1", "other"))
                .isInstanceOf(CartItemNotFoundException.class);
        verify(cartRepository, never()).save(any());
    }

    // ---- clearCart

    @Test
    void clearCart_deletesByUserId() {
        cartService.clearCart("user-1");

        verify(cartRepository).deleteByUserId("user-1");
    }
}