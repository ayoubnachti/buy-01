package com.ecommerce.orderservice.order.dtos.request;

import com.ecommerce.orderservice.order.models.Address;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddressRequest(
        @NotBlank @Size(max = 100) String fullName,
        @NotBlank @Size(max = 30)  String phone,
        @NotBlank @Size(max = 200) String street,
        @NotBlank @Size(max = 100) String city,
        @NotBlank @Size(max = 20)  String postalCode,
        @NotBlank @Size(max = 100) String country) {

    public Address toModel() {
        return new Address(fullName, phone, street, city, postalCode, country);
    }
}
