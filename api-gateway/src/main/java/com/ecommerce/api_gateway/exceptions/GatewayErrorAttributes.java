package com.ecommerce.api_gateway.exceptions;

import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.context.request.WebRequest;

import java.time.Instant;
import java.util.Map;

@Component
public class GatewayErrorAttributes extends DefaultErrorAttributes {

    @Override
    public Map<String, Object> getErrorAttributes(WebRequest webRequest, ErrorAttributeOptions options) {
        Map<String, Object> errorAttributes = super.getErrorAttributes(webRequest, options);
        Throwable error = getError(webRequest);

        if (error != null) {
            String msg = error.getMessage() != null ? error.getMessage() : "";

            if (msg.contains("Unable to find instance") || msg.contains("No instances available")) {
                errorAttributes.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
                errorAttributes.put("message", "Requested service is currently unavailable.");
                errorAttributes.put("timestamp", Instant.now().toString());
            } else if (error instanceof HttpServerErrorException httpServerErrorException) {
                errorAttributes.put("status", httpServerErrorException.getStatusCode().value());
                errorAttributes.put("message", httpServerErrorException.getMessage());
                errorAttributes.put("timestamp", Instant.now().toString());
            }
        }
        return errorAttributes;
    }
}
