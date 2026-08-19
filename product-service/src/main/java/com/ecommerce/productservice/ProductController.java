package com.ecommerce.productservice;

import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.productservice.dtos.ProductResponse;

import lombok.AllArgsConstructor;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;



@RestController
@RequestMapping("/products")
@AllArgsConstructor
public class ProductController {
  private final ProductService productService;

  @GetMapping()
  public List<ProductResponse> getALlProducts() {
    return this.productService.getAllProducts();
  }

  @GetMapping("/{id}")
  public ProductResponse getMethodName(@PathVariable String id) {
      return productService.getProductById(id);
  }
  
  
}
