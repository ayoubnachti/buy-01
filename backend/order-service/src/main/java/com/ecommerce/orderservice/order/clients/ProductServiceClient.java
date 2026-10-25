package com.ecommerce.orderservice.order.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.ecommerce.orderservice.common.ResponseData;
import com.ecommerce.orderservice.order.dtos.response.ProductDto;

@FeignClient(name = "product-service")
public interface ProductServiceClient {

    @GetMapping("/products/{id}")
    ResponseData<ProductDto> getProduct(@PathVariable("id") String id);
}
