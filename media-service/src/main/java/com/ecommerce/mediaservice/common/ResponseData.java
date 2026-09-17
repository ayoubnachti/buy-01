package com.ecommerce.mediaservice.common;

import lombok.*;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResponseData<T> {
    private boolean success;
    private String message;
    private T data;

    public static <T> ResponseData<T> success(String message, T data) {
        return new ResponseData<>(true, message, data);
    }
}