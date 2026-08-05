package com.ecommerce.api_gateway.exceptions;

import org.apache.hc.client5.http.HttpHostConnectException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpServerErrorException;

import java.net.ConnectException;
import java.util.Map;

@RestControllerAdvice
public class GlobalErrorHandler {
    @ExceptionHandler(value={HttpHostConnectException.class, ConnectException.class})
    public ResponseEntity<Map<String, String>> handleConnectionRefused(Exception ex) {
        return ResponseEntity.status((HttpStatusCode)HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "status", "" + HttpStatus.SERVICE_UNAVAILABLE.value(),
                "message", "Service is temporarily unavailable. Please try again later.",
                "timestamp", String.valueOf(System.currentTimeMillis()))
        );
    }


    @ExceptionHandler(HttpServerErrorException.class)
    public ResponseEntity<Map<String, Object>> handleHttpServerError(HttpServerErrorException ex) {

        if (ex.getMessage() != null &&
                ex.getMessage().contains("Unable to find instance")) {

            return ResponseEntity
                    .status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of(
                            "status", HttpStatus.SERVICE_UNAVAILABLE.value(),
                            "message", "Requested service is currently unavailable",
                            "timestamp", String.valueOf(System.currentTimeMillis())
                    ));
        }

        return ResponseEntity
                .status(ex.getStatusCode())
                .body(Map.of(
                        "success", false,
                        "message", ex.getMessage()
                ));
    }
}
