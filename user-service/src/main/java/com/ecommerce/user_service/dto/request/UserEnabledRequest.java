package com.ecommerce.user_service.dto.request;

import jakarta.validation.constraints.NotNull;

public record UserEnabledRequest(
        @NotNull
        Boolean enabled
) {
}
