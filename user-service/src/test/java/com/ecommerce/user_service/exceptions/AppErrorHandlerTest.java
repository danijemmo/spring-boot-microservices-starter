package com.ecommerce.user_service.exceptions;

import com.ecommerce.user_service.dto.APIResponse.APIResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class AppErrorHandlerTest {

    private final AppErrorHandler handler = new AppErrorHandler();

    @Test
    void keycloakAuthExceptionReturnsGenericMessageWithoutBody() {
        ResponseEntity<APIResponse<Object>> response = handler.handleKeycloakAuthException(
                new KeycloakAuthException(
                        HttpStatus.UNAUTHORIZED.value(),
                        "keycloak_auth_error",
                        "Invalid user credentials from Keycloak"
                )
        );

        APIResponse<Object> payload = response.getBody();

        assertNotNull(payload);
        assertEquals(HttpStatus.UNAUTHORIZED.value(), payload.statusCode());
        assertEquals("Authentication failed", payload.message());
        assertNull(payload.body());
    }

    @Test
    void invalidGrantReturnsInvalidCredentialsWithoutBody() {
        ResponseEntity<APIResponse<Object>> response = handler.handleKeycloakAuthException(
                new KeycloakAuthException(
                        HttpStatus.BAD_REQUEST.value(),
                        "invalid_grant",
                        "Invalid user credentials",
                        "Invalid email or password"
                )
        );

        APIResponse<Object> payload = response.getBody();

        assertNotNull(payload);
        assertEquals(HttpStatus.BAD_REQUEST.value(), payload.statusCode());
        assertEquals("Invalid email or password", payload.message());
        assertNull(payload.body());
    }

    @Test
    void refreshInvalidGrantReturnsInvalidRefreshTokenWithoutBody() {
        ResponseEntity<APIResponse<Object>> response = handler.handleKeycloakAuthException(
                new KeycloakAuthException(
                        HttpStatus.BAD_REQUEST.value(),
                        "invalid_grant",
                        "Invalid refresh token",
                        "Invalid refresh token"
                )
        );

        APIResponse<Object> payload = response.getBody();

        assertNotNull(payload);
        assertEquals(HttpStatus.BAD_REQUEST.value(), payload.statusCode());
        assertEquals("Invalid refresh token", payload.message());
        assertNull(payload.body());
    }

    @Test
    void nonValidationErrorsDoNotReturnBody() {
        ResponseEntity<APIResponse<Object>> response = handler.handleNotFoundException(
                new NotFoundException("user not found")
        );

        APIResponse<Object> payload = response.getBody();

        assertNotNull(payload);
        assertEquals(HttpStatus.NOT_FOUND.value(), payload.statusCode());
        assertEquals("user not found", payload.message());
        assertNull(payload.body());
    }
}
