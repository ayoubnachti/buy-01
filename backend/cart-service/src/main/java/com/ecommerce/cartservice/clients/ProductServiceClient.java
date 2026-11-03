package com.ecommerce.cartservice.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.ecommerce.cartservice.dtos.response.ResponseData;
import com.ecommerce.cartservice.dtos.response.ProductDto;

@FeignClient(name = "product-service")
public interface ProductServiceClient {

    @GetMapping("/products/{id}")
    ResponseData<ProductDto> getProduct(@PathVariable("id") String id);
}
