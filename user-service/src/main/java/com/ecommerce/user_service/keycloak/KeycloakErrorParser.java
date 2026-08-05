package com.ecommerce.user_service.keycloak;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class KeycloakErrorParser {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private KeycloakErrorParser() {
    }

    public static ParsedKeycloakError parse(String responseBody, String defaultError, String defaultDescription) {
        String error = defaultError;
        String description = defaultDescription;

        try {
            JsonNode root = OBJECT_MAPPER.readTree(responseBody);
            error = root.path("error").asText(error);
            description = firstText(root, description, "error_description", "errorMessage", "message");
        } catch (Exception e) {
            if (responseBody != null && !responseBody.isBlank()) {
                description = responseBody;
            }
        }

        return new ParsedKeycloakError(error, description);
    }

    private static String firstText(JsonNode root, String fallback, String... fields) {
        for (String field : fields) {
            JsonNode value = root.path(field);
            if (value.isTextual() && !value.asText().isBlank()) {
                return value.asText();
            }
        }

        return fallback;
    }

    public record ParsedKeycloakError(String error, String description) {
    }
}
