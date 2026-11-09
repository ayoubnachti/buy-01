package com.ecommerce.orderservice.order.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.ecommerce.orderservice.common.ResponseData;
import com.ecommerce.orderservice.order.dtos.request.StockChangeRequest;
import com.ecommerce.orderservice.order.dtos.response.ProductDto;

@FeignClient(name = "product-service")
public interface ProductServiceClient {

    @GetMapping("/products/{id}")
    ResponseData<ProductDto> getProduct(@PathVariable("id") String id);

    @PostMapping("/internal/products/stock/reserve")
    void reserveStock(@RequestBody StockChangeRequest request);

    @PostMapping("/internal/products/stock/release")
    void releaseStock(@RequestBody StockChangeRequest request);
}
