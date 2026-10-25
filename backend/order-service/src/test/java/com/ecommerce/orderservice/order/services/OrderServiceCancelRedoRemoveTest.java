package com.ecommerce.orderservice.order.services;

import com.ecommerce.orderservice.common.ResponseData;
import com.ecommerce.orderservice.exceptions.orderExceptions.OrderAlreadyCancelledException;
import com.ecommerce.orderservice.exceptions.orderExceptions.OrderAlreadyDeliveredException;
import com.ecommerce.orderservice.exceptions.orderExceptions.OrderAlreadyShippedException;
import com.ecommerce.orderservice.exceptions.orderExceptions.OrderNotFoundException;
import com.ecommerce.orderservice.exceptions.orderExceptions.OrderStillActiveException;
import com.ecommerce.orderservice.order.clients.ProductServiceGateway;
import com.ecommerce.orderservice.order.dtos.response.OrderResponse;
import com.ecommerce.orderservice.order.dtos.response.ProductDto;
import com.ecommerce.orderservice.order.models.Address;
import com.ecommerce.orderservice.order.models.Order;
import com.ecommerce.orderservice.order.models.OrderItem;
import com.ecommerce.orderservice.order.models.OrderStatus;
import com.ecommerce.orderservice.order.repositories.OrderRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceCancelRedoRemoveTest {

    @Mock
    ProductServiceGateway productServiceGateway;

    @Mock
    OrderRepository orderRepository;

    @InjectMocks
    OrderService orderService;

    private Order orderWithStatus(OrderStatus status) {
        Order order = new Order();
        order.setUserId("client-1");
        order.setSellerId("seller-1");
        order.setShippingAddress(new Address("Test", "+212600000000", "1 Main St", "Casablanca", "20000", "Morocco"));
        order.setItems(List.of(new OrderItem("product-1", "Mouse", "ELECTRONICS", new BigDecimal("9.99"), 1, null)));
        order.changeStatus(status, "client-1");
        return order;
    }

    // --- cancel ---

    @Test
    void ownerCanCancelPendingOrder() {
        Order order = orderWithStatus(OrderStatus.PENDING);
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        ResponseData<OrderResponse> result = orderService.cancel("order-1", "client-1");

        assertThat(result.getData().status()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void cancelAlreadyCancelledOrder_throws() {
        Order order = orderWithStatus(OrderStatus.CANCELLED);
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancel("order-1", "client-1"))
                .isInstanceOf(OrderAlreadyCancelledException.class);
    }

    @Test
    void cancelDeliveredOrder_throws() {
        Order order = orderWithStatus(OrderStatus.DELIVERED);
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancel("order-1", "client-1"))
                .isInstanceOf(OrderAlreadyDeliveredException.class);
    }

    @Test
    void cancelShippedOrder_throws() {
        Order order = orderWithStatus(OrderStatus.SHIPPED);
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancel("order-1", "client-1"))
                .isInstanceOf(OrderAlreadyShippedException.class);
    }

    @Test
    void anotherClientCannotCancel_throwsNotFound() {
        Order order = orderWithStatus(OrderStatus.PENDING);
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancel("order-1", "client-2"))
                .isInstanceOf(OrderNotFoundException.class);
    }

    // --- redo ---

    @Test
    void redoCancelledOrder_createsNewOrder() {
        Order oldOrder = orderWithStatus(OrderStatus.CANCELLED);
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(oldOrder));

        ProductDto product = new ProductDto(
                "product-1", "Mouse", "desc", new BigDecimal("9.99"), 10, "ELECTRONICS", "seller-1", List.of());
        when(productServiceGateway.getProduct("product-1")).thenReturn(product);
        when(orderRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseData<List<OrderResponse>> result = orderService.redo("order-1", "client-1");

        assertThat(result.getData()).hasSize(1);
        assertThat(result.getData().get(0).status()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void redoPendingOrder_throwsStillActive() {
        Order oldOrder = orderWithStatus(OrderStatus.PENDING);
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(oldOrder));

        assertThatThrownBy(() -> orderService.redo("order-1", "client-1"))
                .isInstanceOf(OrderStillActiveException.class);
    }

    // --- remove ---

    @Test
    void removeDeliveredOrder_marksDeleted() {
        Order order = orderWithStatus(OrderStatus.DELIVERED);
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        orderService.remove("order-1", "client-1");

        assertThat(order.isDeletedByUser()).isTrue();
    }

    @Test
    void removePendingOrder_throwsStillActive() {
        Order order = orderWithStatus(OrderStatus.PENDING);
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.remove("order-1", "client-1"))
                .isInstanceOf(OrderStillActiveException.class);
    }
}
