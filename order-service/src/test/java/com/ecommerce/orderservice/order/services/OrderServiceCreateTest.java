package com.ecommerce.orderservice.order.services;

import com.ecommerce.orderservice.common.ResponseData;
import com.ecommerce.orderservice.exceptions.orderExceptions.OutOfStockException;
import com.ecommerce.orderservice.exceptions.orderExceptions.ProductServiceUnavailableException;
import com.ecommerce.orderservice.order.clients.ProductServiceGateway;
import com.ecommerce.orderservice.order.dtos.request.AddressRequest;
import com.ecommerce.orderservice.order.dtos.request.CreateOrderRequest;
import com.ecommerce.orderservice.order.dtos.request.OrderItemRequest;
import com.ecommerce.orderservice.order.dtos.response.OrderResponse;
import com.ecommerce.orderservice.order.dtos.response.ProductDto;
import com.ecommerce.orderservice.order.repositories.OrderRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceCreateTest {

    @Mock
    ProductServiceGateway productServiceGateway;

    @Mock
    OrderRepository orderRepository;

    @InjectMocks
    OrderService orderService;

    private final AddressRequest address = new AddressRequest(
            "Test", "+212600000000", "1 Main St", "Casablanca", "20000", "Morocco");

    private ProductDto product(String id, String seller, String price, int stock) {
        return new ProductDto(id, "Product " + id, "desc", new BigDecimal(price), stock, seller, List.of());
    }

    private OrderResponse findOrderBySeller(List<OrderResponse> orders, String sellerId) {
        for (OrderResponse order : orders) {
            if (order.sellerId().equals(sellerId)) {
                return order;
            }
        }
        throw new IllegalStateException("No order found for seller " + sellerId);
    }

    @Test
    void twoSellers_createTwoOrders_withServerPrices() {
        when(productServiceGateway.getProduct("p1")).thenReturn(product("p1", "seller-1", "19.99", 10));
        when(productServiceGateway.getProduct("p2")).thenReturn(product("p2", "seller-2", "5.50", 10));
        when(orderRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        List<OrderItemRequest> items = List.of(
                new OrderItemRequest("p1", 2),
                new OrderItemRequest("p2", 1));
        CreateOrderRequest request = new CreateOrderRequest(address, items);

        ResponseData<List<OrderResponse>> result = orderService.createOrders("user-1", request);
        List<OrderResponse> orders = result.getData();

        assertThat(orders).hasSize(2);

        OrderResponse seller1Order = findOrderBySeller(orders, "seller-1");
        OrderResponse seller2Order = findOrderBySeller(orders, "seller-2");

        assertThat(seller1Order.totalAmount()).isEqualByComparingTo("39.98");
        assertThat(seller2Order.totalAmount()).isEqualByComparingTo("5.50");
        assertThat(seller1Order.statusHistory()).hasSize(1);
    }

    @Test
    void notEnoughStock_throws() {
        when(productServiceGateway.getProduct("p1")).thenReturn(product("p1", "seller-1", "19.99", 1));
        CreateOrderRequest request = new CreateOrderRequest(address, List.of(new OrderItemRequest("p1", 5)));

        assertThatThrownBy(() -> orderService.createOrders("user-1", request))
                .isInstanceOf(OutOfStockException.class);
    }

    @Test
    void productServiceDown_throwsServiceUnavailable() {
        when(productServiceGateway.getProduct("p1")).thenThrow(new ProductServiceUnavailableException("down"));
        CreateOrderRequest request = new CreateOrderRequest(address, List.of(new OrderItemRequest("p1", 1)));

        assertThatThrownBy(() -> orderService.createOrders("user-1", request))
                .isInstanceOf(ProductServiceUnavailableException.class);
    }
}
