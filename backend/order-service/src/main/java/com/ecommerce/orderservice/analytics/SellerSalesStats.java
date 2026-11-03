package com.ecommerce.orderservice.analytics;

import java.math.BigDecimal;
import java.util.List;

public record SellerSalesStats(
        BigDecimal revenue,
        long unitsSold,
        long ordersCount,
        List<BestSeller> bestSellers,
        List<RevenueByDay> revenueByDay) {

    public record BestSeller(String productId, String name, long units, BigDecimal revenue) {}

    public record RevenueByDay(String date, BigDecimal revenue) {}

    public static SellerSalesStats empty(List<RevenueByDay> revenueByDay) {
        return new SellerSalesStats(BigDecimal.ZERO, 0, 0, List.of(), revenueByDay);
    }
}
