package com.ecommerce.order_service.exceptions;

public class EnumIllegalArgumentException extends RuntimeException {
   public EnumIllegalArgumentException(String valueType, String value, String valuesString) {
      super(
            "Invalid "
                  + valueType
                  + " : "
                  + value
                  + " --- allowed values are "
                  + valuesString
      );
   }
}
