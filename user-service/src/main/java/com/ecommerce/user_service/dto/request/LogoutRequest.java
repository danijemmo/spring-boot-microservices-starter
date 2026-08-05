package com.ecommerce.user_service.dto.request;

public record LogoutRequest(
        String refreshToken
) {
}
