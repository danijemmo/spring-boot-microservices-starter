package com.ecommerce.order_service.exceptions;

public class NotAuthorizedException extends RuntimeException {
   public NotAuthorizedException() {
      super();
   }

   public NotAuthorizedException(String message) {
      super(message);
   }
}
