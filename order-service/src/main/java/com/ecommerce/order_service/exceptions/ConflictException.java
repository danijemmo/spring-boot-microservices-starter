package com.ecommerce.order_service.exceptions;

public class ConflictException extends RuntimeException {
   public ConflictException(String message) {
      super(message);
   }
}
