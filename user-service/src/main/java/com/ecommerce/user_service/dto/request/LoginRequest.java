package com.ecommerce.user_service.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record LoginRequest(
        @NotNull()
        @NotEmpty()
        String email,

        @NotNull()
        @NotEmpty()
        String password
) {
}
