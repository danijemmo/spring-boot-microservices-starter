package com.ecommerce.user_service.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ResetPasswordRequest(
        @NotBlank
        String password
) {
}
