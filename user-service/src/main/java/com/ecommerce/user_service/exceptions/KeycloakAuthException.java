package com.ecommerce.user_service.exceptions;

import lombok.Getter;

@Getter
public class KeycloakAuthException extends RuntimeException {
    private final int statusCode;
    private final String error;
    private final String errorDescription;
    private final String clientMessage;

    public KeycloakAuthException(int statusCode, String error, String errorDescription) {
        this(statusCode, error, errorDescription, null);
    }

    public KeycloakAuthException(int statusCode, String error, String errorDescription, String clientMessage) {
        super(errorDescription);
        this.statusCode = statusCode;
        this.error = error;
        this.errorDescription = errorDescription;
        this.clientMessage = clientMessage;
    }
}
