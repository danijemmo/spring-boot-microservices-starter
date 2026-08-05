package com.ecommerce.user_service.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UserRoleRequest(
        @NotBlank
        String role
) {
}
