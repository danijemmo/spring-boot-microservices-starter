package com.ecommerce.api_gateway.exceptions;

import org.apache.hc.client5.http.HttpHostConnectException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpServerErrorException;

import java.net.ConnectException;
import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class GlobalErrorHandler {

    @ExceptionHandler({HttpHostConnectException.class, ConnectException.class})
    public ResponseEntity<Map<String, Object>> handleConnectionRefused(Exception ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "statusCode", HttpStatus.SERVICE_UNAVAILABLE.value(),
                "message", "Service is temporarily unavailable. Please try again later.",
                "timestamp", Instant.now().toString()
        ));
    }

    @ExceptionHandler(HttpServerErrorException.class)
    public ResponseEntity<Map<String, Object>> handleHttpServerError(HttpServerErrorException ex) {
        if (ex.getMessage() != null && ex.getMessage().contains("Unable to find instance")) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                    "statusCode", HttpStatus.SERVICE_UNAVAILABLE.value(),
                    "message", "Requested microservice is currently unavailable or not registered in Eureka.",
                    "timestamp", Instant.now().toString()
            ));
        }

        return ResponseEntity.status(ex.getStatusCode()).body(Map.of(
                "statusCode", ex.getStatusCode().value(),
                "message", ex.getMessage() != null ? ex.getMessage() : "Downstream service error",
                "timestamp", Instant.now().toString()
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAllExceptions(Exception ex) {
        String msg = ex.getMessage() != null ? ex.getMessage() : "";
        if (msg.contains("Unable to find instance") || msg.contains("No instances available")) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                    "statusCode", HttpStatus.SERVICE_UNAVAILABLE.value(),
                    "message", "Requested service is currently unavailable.",
                    "timestamp", Instant.now().toString()
            ));
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "statusCode", HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "message", msg.isBlank() ? "Internal gateway error" : msg,
                "timestamp", Instant.now().toString()
        ));
    }
}
