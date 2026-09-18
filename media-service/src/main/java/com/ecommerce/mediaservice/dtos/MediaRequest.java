package com.ecommerce.mediaservice.dtos;

import java.util.List;

import jakarta.validation.constraints.NotNull;

public record MediaRequest(
        TargetType targetType,
        @NotNull(message = "Invalid target Id !") String targetId,
        List<String> oldImagePaths) {

}
