package com.ecommerce.user_service.dto.response;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        int expiresIn
) {
}
