package com.ecommerce.cartservice.services;

import java.time.Clock;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.ecommerce.cartservice.models.Cart;
import com.ecommerce.cartservice.models.CartItem;
import com.ecommerce.cartservice.repositories.CartRepository;
import com.ecommerce.cartservice.clients.ProductServiceGateway;
import com.ecommerce.cartservice.dtos.response.CartResponse;
import com.ecommerce.cartservice.dtos.response.ProductDto;
import com.ecommerce.cartservice.exceptions.custom.CartItemNotFoundException;
import com.ecommerce.cartservice.exceptions.custom.OutOfStockException;
import com.ecommerce.cartservice.exceptions.custom.ProductNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final ProductServiceGateway productGateway;
    private final Clock clock;

    public Cart getCart(String userId) {
        return cartRepository.findByUserId(userId).orElseGet(() -> new Cart(userId));
    }

    public Cart addItem(String userId, String productId, int quantity) {
        Cart cart = getCart(userId);
        checkStock(productId, cart.quantityOf(productId) + quantity);
        cart.addItem(productId, quantity, clock.instant());
        return cartRepository.save(cart);
    }

    public Cart updateQuantity(String userId, String productId, int quantity) {
        Cart cart = existingCart(userId, productId);
        if (cart.quantityOf(productId) == 0) {
            throw new CartItemNotFoundException(productId);
        }
        checkStock(productId, quantity);
        cart.updateQuantity(productId, quantity, clock.instant());
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

    public CartResponse describe(Cart cart) {
        Set<String> ids = cart.getItems().stream().map(CartItem::getProductId).collect(Collectors.toSet());
        Map<String, ProductDto> products = ids.isEmpty() ? Map.of() : productGateway.getProducts(ids);
        return CartResponse.from(cart, products);
    }

    private void checkStock(String productId, int wantedQuantity) {
        ProductDto product = productGateway.getProduct(productId);
        if (product == null) {
            throw new ProductNotFoundException(productId);
        }
        if (wantedQuantity > product.quantity()) {
            throw new OutOfStockException(productId, product.quantity());
        }
    }

    private Cart existingCart(String userId, String productId) {
        return cartRepository.findByUserId(userId)
                .orElseThrow(() -> new CartItemNotFoundException(productId));
    }
}