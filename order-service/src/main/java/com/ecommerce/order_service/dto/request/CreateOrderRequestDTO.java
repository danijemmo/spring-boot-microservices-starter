package com.ecommerce.order_service.dto.request;

import jakarta.validation.constraints.NotNull;

public record CreateOrderRequestDTO(
        @NotNull
        String userId,

        @NotNull
        String amount
) {
}
