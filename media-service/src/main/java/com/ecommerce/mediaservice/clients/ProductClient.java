package com.ecommerce.mediaservice.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.ecommerce.mediaservice.dtos.Product;

@FeignClient(name = "product-service")
public interface ProductClient {
    @GetMapping("/product/{id}")
    public Product getProduct(@PathVariable("id") String id);
}
