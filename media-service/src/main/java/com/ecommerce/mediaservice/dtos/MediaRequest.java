package com.ecommerce.mediaservice.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record MediaRequest(
        @NotNull(message = "Invalid target type !")
        @Pattern(regexp = "product|profile", message = "Invalid target type !") String targetType,

        @NotNull(message = "Invalid target Id !") String targetId) {

}
