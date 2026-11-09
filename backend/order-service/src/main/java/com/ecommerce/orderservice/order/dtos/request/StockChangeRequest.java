package com.ecommerce.orderservice.order.dtos.request;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Body sent to product-service's internal stock endpoints. */
public record StockChangeRequest(List<StockItem> items) {

    public record StockItem(String productId, int quantity) {
    }

    public static StockChangeRequest of(Map<String, Integer> quantityByProduct) {
        List<StockItem> items = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : quantityByProduct.entrySet()) {
            items.add(new StockItem(entry.getKey(), entry.getValue()));
        }
        return new StockChangeRequest(items);
    }
}
