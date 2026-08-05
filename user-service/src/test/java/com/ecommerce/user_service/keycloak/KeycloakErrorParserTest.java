package com.ecommerce.user_service.keycloak;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KeycloakErrorParserTest {

    @Test
    void parsesAdminErrorMessage() {
        KeycloakErrorParser.ParsedKeycloakError error = KeycloakErrorParser.parse(
                "{\"errorMessage\":\"User exists with same email\"}",
                "keycloak_admin_error",
                "Failed to create user in Keycloak"
        );

        assertEquals("keycloak_admin_error", error.error());
        assertEquals("User exists with same email", error.description());
    }

    @Test
    void parsesAuthErrorDescription() {
        KeycloakErrorParser.ParsedKeycloakError error = KeycloakErrorParser.parse(
                "{\"error\":\"invalid_grant\",\"error_description\":\"Invalid user credentials\"}",
                "keycloak_auth_error",
                "Authentication failed"
        );

        assertEquals("invalid_grant", error.error());
        assertEquals("Invalid user credentials", error.description());
    }
}
