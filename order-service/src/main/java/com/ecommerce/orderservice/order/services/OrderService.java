package com.ecommerce.orderservice.order.services;

import com.ecommerce.orderservice.common.ResponseData;
import com.ecommerce.orderservice.exceptions.orderExceptions.CannotOrderOwnProductException;
import com.ecommerce.orderservice.exceptions.orderExceptions.OutOfStockException;
import com.ecommerce.orderservice.exceptions.orderExceptions.ProductNotFoundException;
import com.ecommerce.orderservice.order.clients.ProductServiceGateway;
import com.ecommerce.orderservice.order.dtos.request.CreateOrderRequest;
import com.ecommerce.orderservice.order.dtos.request.OrderItemRequest;
import com.ecommerce.orderservice.order.dtos.response.OrderResponse;
import com.ecommerce.orderservice.order.dtos.response.ProductDto;
import com.ecommerce.orderservice.order.models.Order;
import com.ecommerce.orderservice.order.models.OrderItem;
import com.ecommerce.orderservice.order.models.OrderStatus;
import com.ecommerce.orderservice.order.repositories.OrderRepository;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductServiceGateway productServiceGateway;

    public ResponseData<List<OrderResponse>> createOrders(String userId, CreateOrderRequest request) {

        // Step 1: if the same product appears twice in the request, add the quantities together.
        Map<String, Integer> quantityByProduct = new HashMap<>();
        for (OrderItemRequest itemRequest : request.items()) {
            String productId = itemRequest.productId();
            int quantity = itemRequest.quantity();

            if (quantityByProduct.containsKey(productId)) {
                int existingQuantity = quantityByProduct.get(productId);
                quantityByProduct.put(productId, existingQuantity + quantity);
            } else {
                quantityByProduct.put(productId, quantity);
            }
        }

        // Step 2: check every product first. Nothing is saved yet.
        Map<String, List<OrderItem>> itemsBySeller = new HashMap<>();

        for (Map.Entry<String, Integer> entry : quantityByProduct.entrySet()) {
            String productId = entry.getKey();
            int quantity = entry.getValue();

            ProductDto product = productServiceGateway.getProduct(productId);
            if (product == null) {
                throw new ProductNotFoundException("Product not found: " + productId);
            }

            if (userId.equals(product.userId())) {
                throw new CannotOrderOwnProductException("You cannot buy your own product: " + product.name());
            }

            if (product.quantity() != null && product.quantity() < quantity) {
                throw new OutOfStockException("Not enough stock for: " + product.name());
            }

            // OrderItem.imageUrl is one String. product.imageUrls() is a list of images.
            // We just keep the first image, or none.
            String imageUrl = null;
            if (product.imageUrls() != null && !product.imageUrls().isEmpty()) {
                imageUrl = product.imageUrls().get(0);
            }

            OrderItem item = new OrderItem(
                    product.id(),
                    product.name(),
                    product.price(),
                    quantity,
                    imageUrl);

            String sellerId = product.userId();
            List<OrderItem> sellerItems = itemsBySeller.get(sellerId);
            if (sellerItems == null) {
                sellerItems = new ArrayList<>();
                itemsBySeller.put(sellerId, sellerItems);
            }
            sellerItems.add(item);
        }

        // Step 3: build one order per seller.
        List<Order> orders = new ArrayList<>();
        for (Map.Entry<String, List<OrderItem>> entry : itemsBySeller.entrySet()) {
            String sellerId = entry.getKey();
            List<OrderItem> items = entry.getValue();

            Order order = new Order();
            order.setUserId(userId);
            order.setSellerId(sellerId);
            order.setItems(items);
            order.setShippingAddress(request.shippingAddress().toModel());
            order.setTotalAmount(computeTotal(items));
            order.changeStatus(OrderStatus.PENDING, userId);

            orders.add(order);
        }

        List<Order> savedOrders = orderRepository.saveAll(orders);

        // Step 4: turn the saved orders into the response shape.
        List<OrderResponse> response = new ArrayList<>();
        for (Order order : savedOrders) {
            response.add(OrderResponse.from(order, false));
        }

        return ResponseData.success("Order created successfully", response);
    }

    private BigDecimal computeTotal(List<OrderItem> items) {
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItem item : items) {
            BigDecimal lineTotal = item.unitPrice().multiply(BigDecimal.valueOf(item.quantity()));
            total = total.add(lineTotal);
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }
}
