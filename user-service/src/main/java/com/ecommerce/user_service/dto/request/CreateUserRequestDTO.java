package com.ecommerce.user_service.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

public record CreateUserRequestDTO(
        @NotNull
        String firstName,

        @NotNull
        String lastName,

        @NotNull
        @Email
        String email,

        @NotNull
        String password
) {
}
