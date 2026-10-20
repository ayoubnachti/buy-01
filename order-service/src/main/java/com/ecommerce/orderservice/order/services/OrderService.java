package com.ecommerce.orderservice.order.services;

import com.ecommerce.orderservice.common.ResponseData;
import com.ecommerce.orderservice.exceptions.orderExceptions.CannotOrderOwnProductException;
import com.ecommerce.orderservice.exceptions.orderExceptions.InvalidOrderStatusTransitionException;
import com.ecommerce.orderservice.exceptions.orderExceptions.OrderAlreadyCancelledException;
import com.ecommerce.orderservice.exceptions.orderExceptions.OrderAlreadyDeliveredException;
import com.ecommerce.orderservice.exceptions.orderExceptions.OrderAlreadyShippedException;
import com.ecommerce.orderservice.exceptions.orderExceptions.OrderNotFoundException;
import com.ecommerce.orderservice.exceptions.orderExceptions.OrderStillActiveException;
import com.ecommerce.orderservice.exceptions.orderExceptions.OutOfStockException;
import com.ecommerce.orderservice.exceptions.orderExceptions.ProductNotFoundException;
import com.ecommerce.orderservice.order.clients.ProductServiceGateway;
import com.ecommerce.orderservice.order.dtos.request.AddressRequest;
import com.ecommerce.orderservice.order.dtos.request.CreateOrderRequest;
import com.ecommerce.orderservice.order.dtos.request.OrderItemRequest;
import com.ecommerce.orderservice.order.dtos.response.OrderResponse;
import com.ecommerce.orderservice.order.dtos.response.ProductDto;
import com.ecommerce.orderservice.order.models.Address;
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

    public ResponseData<OrderResponse> getOrder(String orderId, String callerId) {
        Order order = getVisibleOrder(orderId, callerId);
        boolean forSeller = order.getSellerId().equals(callerId);
        return ResponseData.success("Order retrieved successfully", OrderResponse.from(order, forSeller));
    }

    /**
     * Only the owner or the seller can see the order. Everyone else gets a
     * "not found" error, not "forbidden" — so we never reveal that the order exists.
     */
    private Order getVisibleOrder(String orderId, String callerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));

        boolean isOwner = order.getUserId().equals(callerId) && !order.isDeletedByUser();
        boolean isSeller = order.getSellerId().equals(callerId);
        if (!isOwner && !isSeller) {
            throw new OrderNotFoundException("Order not found");
        }

        return order;
    }

    public ResponseData<OrderResponse> updateStatusAsSeller(String orderId, String sellerId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));

        if (!order.getSellerId().equals(sellerId)) {
            throw new OrderNotFoundException("Order not found");
        }

        if (!canMoveToNextStatus(order.getStatus(), newStatus)) {
            throw new InvalidOrderStatusTransitionException(
                    "Cannot change status from " + order.getStatus() + " to " + newStatus);
        }

        order.changeStatus(newStatus, sellerId);
        Order savedOrder = orderRepository.save(order); // @Version protects against two updates at once

        return ResponseData.success("Order status updated successfully", OrderResponse.from(savedOrder, true));
    }

    /** The owner or the seller can cancel, only while the order is PENDING or CONFIRMED. */
    public ResponseData<OrderResponse> cancel(String orderId, String callerId) {
        Order order = getVisibleOrder(orderId, callerId);

        if (!canCancel(order.getStatus())) {
            if (order.getStatus() == OrderStatus.CANCELLED) {
                throw new OrderAlreadyCancelledException("This order is already cancelled");
            }
            if (order.getStatus() == OrderStatus.DELIVERED) {
                throw new OrderAlreadyDeliveredException("This order was already delivered");
            }
            throw new OrderAlreadyShippedException("This order has already shipped");
        }

        order.changeStatus(OrderStatus.CANCELLED, callerId);
        Order savedOrder = orderRepository.save(order);

        boolean forSeller = savedOrder.getSellerId().equals(callerId);
        return ResponseData.success("Order cancelled successfully", OrderResponse.from(savedOrder, forSeller));
    }

    /** Only the owner. Makes a NEW order with the same products; price and stock are checked again. */
    public ResponseData<List<OrderResponse>> redo(String orderId, String userId) {
        Order oldOrder = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));

        if (!oldOrder.getUserId().equals(userId) || oldOrder.isDeletedByUser()) {
            throw new OrderNotFoundException("Order not found");
        }

        if (!canRedoOrRemove(oldOrder.getStatus())) {
            throw new OrderStillActiveException("Only CANCELLED or DELIVERED orders can be ordered again");
        }

        List<OrderItemRequest> items = new ArrayList<>();
        for (OrderItem item : oldOrder.getItems()) {
            items.add(new OrderItemRequest(item.productId(), item.quantity()));
        }

        Address oldAddress = oldOrder.getShippingAddress();
        AddressRequest address = new AddressRequest(oldAddress.fullName(), oldAddress.phone(), oldAddress.street(),
                oldAddress.city(), oldAddress.postalCode(), oldAddress.country());

        return createOrders(userId, new CreateOrderRequest(address, items)); // same checks as a new order
    }

    /** Soft delete: the owner stops seeing it, but dashboards still count it. */
    public void remove(String orderId, String userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));

        if (!order.getUserId().equals(userId) || order.isDeletedByUser()) {
            throw new OrderNotFoundException("Order not found");
        }

        if (!canRedoOrRemove(order.getStatus())) {
            throw new OrderStillActiveException("Only CANCELLED or DELIVERED orders can be removed");
        }

        order.setDeletedByUser(true);
        orderRepository.save(order);
    }

    private boolean canCancel(OrderStatus status) {
        return status == OrderStatus.PENDING || status == OrderStatus.CONFIRMED;
    }

    private boolean canRedoOrRemove(OrderStatus status) {
        return status == OrderStatus.CANCELLED || status == OrderStatus.DELIVERED;
    }

    /** A seller can only move one step forward: PENDING -> CONFIRMED -> SHIPPED -> DELIVERED. */
    private boolean canMoveToNextStatus(OrderStatus from, OrderStatus to) {
        if (from == OrderStatus.PENDING && to == OrderStatus.CONFIRMED) {
            return true;
        }
        if (from == OrderStatus.CONFIRMED && to == OrderStatus.SHIPPED) {
            return true;
        }
        if (from == OrderStatus.SHIPPED && to == OrderStatus.DELIVERED) {
            return true;
        }
        return false;
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
