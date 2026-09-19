package com.ecommerce.mediaservice.dtos;

import jakarta.validation.constraints.NotNull;

public record MediaRequest(
        TargetType targetType,
        @NotNull(message = "Invalid target Id !") String targetId) {

}
