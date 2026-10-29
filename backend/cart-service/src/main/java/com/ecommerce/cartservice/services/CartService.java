package com.ecommerce.cartservice.services;

import java.time.Clock;

import org.springframework.stereotype.Service;

import com.ecommerce.cartservice.models.Cart;
import com.ecommerce.cartservice.repositories.CartRepository;
import com.ecommerce.cartservice.exceptions.custom.CartItemNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final Clock clock;

    public Cart getCart(String userId) {
        return cartRepository.findByUserId(userId).orElseGet(() -> new Cart(userId));
    }

    public Cart addItem(String userId, String productId, int quantity) {
        Cart cart = getCart(userId);
        cart.addItem(productId, quantity, clock.instant());
        return cartRepository.save(cart);
    }

    public Cart updateQuantity(String userId, String productId, int quantity) {
        Cart cart = existingCart(userId, productId);
        if (!cart.updateQuantity(productId, quantity, clock.instant())) {
            throw new CartItemNotFoundException(productId);
        }
        return cartRepository.save(cart);
    }

    public Cart removeItem(String userId, String productId) {
        Cart cart = existingCart(userId, productId);
        if (!cart.removeItem(productId, clock.instant())) {
            throw new CartItemNotFoundException(productId);
        }
        return cartRepository.save(cart);
    }

    public void clearCart(String userId) {
        cartRepository.deleteByUserId(userId);
    }

    private Cart existingCart(String userId, String productId) {
        return cartRepository.findByUserId(userId)
                .orElseThrow(() -> new CartItemNotFoundException(productId));
    }
}