package com.ecommerce.user_service.dto.APIResponse;


import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.ResponseEntity;

import java.util.Date;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record APIResponse<T>(
      int statusCode,
      String message,
      Boolean deprecated,
      T body,
      Date timestamp
) {

   public static <T> ResponseEntity<APIResponse<T>> build(int statusCode, String message, T body) {
      return ResponseEntity
            .status(statusCode)
            .body(new APIResponse<T>(statusCode, message, null, body, new Date()));
   }

   public static <T> ResponseEntity<APIResponse<T>> build(int statusCode, String message, boolean depricated, T body) {
      return ResponseEntity
            .status(statusCode)
            .body(new APIResponse<T>(statusCode, message, depricated, body, new Date()));
   }
}
