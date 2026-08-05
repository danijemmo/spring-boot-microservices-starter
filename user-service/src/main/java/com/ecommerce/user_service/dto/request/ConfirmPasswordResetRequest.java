package com.ecommerce.user_service.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ConfirmPasswordResetRequest(
        @NotBlank
        String token,

        @NotBlank
        String password
) {
}
