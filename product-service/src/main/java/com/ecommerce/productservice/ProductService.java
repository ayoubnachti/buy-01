package com.ecommerce.productservice;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.ecommerce.productservice.dtos.ProductResponse;
import com.ecommerce.productservice.dtos.UserResponse;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class ProductService {
  private final RestTemplate restTemplate = new RestTemplate();

  private final List<ProductResponse> products = List.of(
    new ProductResponse("1","product 1", "product 1 description" ,10.,"1"),
    new ProductResponse("2","product 2", "product 2 description" ,10.,"2"),
    new ProductResponse("3","product 3", "product 3 description" ,10.,"3"),
    new ProductResponse("4","product 4", "product 4 description" ,10.,"1"),
    new ProductResponse("5","product 5", "product 5 description" ,10.,"1"),
    new ProductResponse("6","product 6", "product 6 description" ,10.,"3"),
    new ProductResponse("7","product 7", "product 7 description" ,10.,"3")
  );

  public List<ProductResponse> getAllProducts() {
    for (ProductResponse product : products) {
      UserResponse seller = getUser(product.userId());
      System.out.println("asdf asf ");
      System.out.printf("%s seller is: %s \n", product.name(), seller.name()); 
    }
    return this.products;
  }
  

  public UserResponse getUser(String userId) {
        return restTemplate.getForObject(
            "http://localhost:8081/users/" + userId,
            UserResponse.class
        );
    }
}
