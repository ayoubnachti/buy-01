package com.ecommerce.orderservice.analytics;

import java.math.BigDecimal;
import java.util.List;

public record UserPurchaseStats(
        BigDecimal totalSpent,
        long ordersCount,
        List<MostBoughtProduct> mostBought,
        List<TopCategory> topCategories) {

    public record MostBoughtProduct(String productId, String name, long units, BigDecimal amount) {}

    public record TopCategory(String category, long units, BigDecimal amount) {}

    public static UserPurchaseStats empty() {
        return new UserPurchaseStats(BigDecimal.ZERO, 0, List.of(), List.of());
    }
}
