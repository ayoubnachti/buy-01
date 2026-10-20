package com.ecommerce.orderservice.order.services;

import com.ecommerce.orderservice.common.ResponseData;
import com.ecommerce.orderservice.exceptions.orderExceptions.InvalidOrderStatusTransitionException;
import com.ecommerce.orderservice.exceptions.orderExceptions.OrderNotFoundException;
import com.ecommerce.orderservice.order.clients.ProductServiceGateway;
import com.ecommerce.orderservice.order.dtos.response.OrderResponse;
import com.ecommerce.orderservice.order.models.Order;
import com.ecommerce.orderservice.order.models.OrderStatus;
import com.ecommerce.orderservice.order.repositories.OrderRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceStatusTest {

    @Mock
    ProductServiceGateway productServiceGateway;

    @Mock
    OrderRepository orderRepository;

    @InjectMocks
    OrderService orderService;

    private Order pendingOrder() {
        Order order = new Order();
        order.setUserId("client-1");
        order.setSellerId("seller-1");
        order.changeStatus(OrderStatus.PENDING, "client-1");
        return order;
    }

    @Test
    void sellerCanMovePendingToConfirmed() {
        Order order = pendingOrder();
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        ResponseData<OrderResponse> result = orderService.updateStatusAsSeller("order-1", "seller-1", OrderStatus.CONFIRMED);

        assertThat(result.getData().status()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void sellerCannotSkipAStatus() {
        Order order = pendingOrder();
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateStatusAsSeller("order-1", "seller-1", OrderStatus.SHIPPED))
                .isInstanceOf(InvalidOrderStatusTransitionException.class);
    }

    @Test
    void anotherSellerCannotUpdateStatus() {
        Order order = pendingOrder();
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateStatusAsSeller("order-1", "seller-2", OrderStatus.CONFIRMED))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void orderNotFound_throwsOrderNotFoundException() {
        when(orderRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.updateStatusAsSeller("missing", "seller-1", OrderStatus.CONFIRMED))
                .isInstanceOf(OrderNotFoundException.class);
    }
}
