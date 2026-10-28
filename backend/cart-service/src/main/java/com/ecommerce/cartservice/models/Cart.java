package com.ecommerce.cartservice.models;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Document(collection = "carts")
public class Cart {

    @Id
    private String id;

    @Indexed(unique = true)          // one cart per user
    private String userId;

    private List<CartItem> items = new ArrayList<>();

    private Instant updatedAt;

    @Version                         // two concurrent saves of the same cart -> the second one fails instead of overwriting
    private Long version;

    public Cart(String userId) {
        this.userId = userId;
    }

    /** Adds the product, or increases its quantity if it is already in the cart. */
    public void addItem(String productId, int quantity, Instant now) {
        findItem(productId).ifPresentOrElse(
                item -> item.setQuantity(item.getQuantity() + quantity),
                () -> items.add(new CartItem(productId, quantity, now)));
        updatedAt = now;
    }

    /** @return false if the product is not in the cart */
    public boolean updateQuantity(String productId, int quantity, Instant now) {
        Optional<CartItem> item = findItem(productId);
        item.ifPresent(i -> i.setQuantity(quantity));
        if (item.isPresent()) {
            updatedAt = now;
        }
        return item.isPresent();
    }

    /** @return false if the product is not in the cart */
    public boolean removeItem(String productId, Instant now) {
        boolean removed = items.removeIf(i -> i.getProductId().equals(productId));
        if (removed) {
            updatedAt = now;
        }
        return removed;
    }

    private Optional<CartItem> findItem(String productId) {
        return items.stream().filter(i -> i.getProductId().equals(productId)).findFirst();
    }
}