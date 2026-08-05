package com.ecommerce.user_service.exceptions;

import com.ecommerce.user_service.dto.APIResponse.APIResponse;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mail.MailSendException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
//import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
//import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class AppErrorHandler {
   static final Logger logger = LoggerFactory.getLogger(AppErrorHandler.class);

   @ExceptionHandler(value = {StatusRuntimeException.class})
   public ResponseEntity<APIResponse<Object>> handleGrpcException(StatusRuntimeException e) {
      Status.Code code = e.getStatus().getCode();
      String description = e.getStatus().getDescription();

      HttpStatus httpStatus = switch (code) {
         case NOT_FOUND -> HttpStatus.NOT_FOUND;
         case INVALID_ARGUMENT -> HttpStatus.BAD_REQUEST;
         case ALREADY_EXISTS -> HttpStatus.CONFLICT;
         case PERMISSION_DENIED -> HttpStatus.FORBIDDEN;
         case UNAUTHENTICATED -> HttpStatus.UNAUTHORIZED;
         case UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
         case DEADLINE_EXCEEDED -> HttpStatus.GATEWAY_TIMEOUT;
         default -> HttpStatus.INTERNAL_SERVER_ERROR;
      };

      return APIResponse.build(
              httpStatus.value(),
              description != null ? description : "Error communicating with downstream service",
              null
      );
   }

    @ExceptionHandler(value = {KeycloakAuthException.class})
    public ResponseEntity<APIResponse<Object>> handleKeycloakAuthException(KeycloakAuthException e) {
       logger.warn("Identity provider request failed with status {} and error {}", e.getStatusCode(), e.getError());
       return APIResponse.build(
               e.getStatusCode(),
               keycloakClientMessage(e),
               null
       );
    }

   @ExceptionHandler(value = {NoResourceFoundException.class})
   public ResponseEntity<APIResponse<Object>> handleNoResourceFoundException(NoResourceFoundException e) {
      logger.info("Unknown endpoint requested {}", e.getResourcePath());
      return APIResponse.build(
            HttpStatus.BAD_REQUEST.value(),
            "Endpoint " + e.getResourcePath() + " is not found.",
            null
      );
   }

   @ExceptionHandler(value = {NotAuthorizedException.class})
   public ResponseEntity<APIResponse<Object>> handleNotAuthorizedException(NotAuthorizedException e) {
      return APIResponse.build(
            HttpStatus.UNAUTHORIZED.value(),
            e.getMessage(),
            null
      );
   }

   @ExceptionHandler(value = {AuthorizationDeniedException.class})
   public ResponseEntity<APIResponse<Object>> handleAuthorizationDeniedException(AuthorizationDeniedException e) {
      return APIResponse.build(
              HttpStatus.FORBIDDEN.value(),
              e.getMessage(),
              null
      );
   }

    @ExceptionHandler(value = {NotFoundException.class})
    public ResponseEntity<APIResponse<Object>> handleNotFoundException(NotFoundException e) {
      return APIResponse.build(
            HttpStatus.NOT_FOUND.value(),
            e.getMessage(),
            null
      );
    }

   @ExceptionHandler(value = {InvalidTokenException.class})
   public ResponseEntity<APIResponse<Object>> handleInvalidTokenException(InvalidTokenException e) {
      return APIResponse.build(
            HttpStatus.BAD_REQUEST.value(),
            e.getMessage(),
            null
      );
   }

   @ExceptionHandler(value = {ConflictException.class})
   public ResponseEntity<APIResponse<Object>> handleConflictException(ConflictException e) {
      return APIResponse.build(
            HttpStatus.CONFLICT.value(),
            e.getMessage(),
            null
      );
   }

   @ExceptionHandler(value = {IllegalArgumentException.class})
   public ResponseEntity<APIResponse<Object>> handleNotFoundException(IllegalArgumentException e) {
      return APIResponse.build(
            HttpStatus.BAD_REQUEST.value(),
            e.getMessage(),
            null
      );
   }

   @ExceptionHandler(value = {EnumIllegalArgumentException.class})
   public ResponseEntity<APIResponse<Object>> handleNotFoundException(EnumIllegalArgumentException e) {
      return APIResponse.build(
            HttpStatus.BAD_REQUEST.value(),
            e.getMessage(),
            null
      );
   }

   @ExceptionHandler(value = {HttpRequestMethodNotSupportedException.class})
   public ResponseEntity<APIResponse<Object>> handleHttpRequestMethodNotSupportedException(
         HttpRequestMethodNotSupportedException e) {
      logger.info("Unknown method requested {}", e.getMethod());
      return APIResponse.build(
            HttpStatus.BAD_REQUEST.value(),
            "Method " + e.getMethod() + " is not found on this endpoint.",
            null
      );
   }

   //   @ResponseStatus(HttpStatus.BAD_REQUEST)
   @ExceptionHandler(MethodArgumentNotValidException.class)
   public ResponseEntity<APIResponse<Map<String, String>>> handleValidationExceptions(MethodArgumentNotValidException ex) {

      Map<String, String> errors = new HashMap<>();

      ex.getBindingResult().getAllErrors().forEach((error) -> {
         if (error instanceof FieldError) {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
         } else {
            String fieldName = error.getObjectName();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
         }
      });

      return APIResponse.build(
            HttpStatus.BAD_REQUEST.value(),
            "Validation error",
            errors
      );
   }

   @ExceptionHandler(MethodArgumentTypeMismatchException.class)
   public ResponseEntity<APIResponse<Map<String, String>>> handleMethodValidationExceptions(MethodArgumentTypeMismatchException ex) {

      if (ex.getRequiredType() == YearMonth.class) {
         return APIResponse.build(
                 HttpStatus.BAD_REQUEST.value(),
                 "Invalid month format. Please use 'YYYY-MM'.",
                 null
         );
      }

      Map<String, String> errors = new HashMap<>();

      errors.put(
            ex.getParameter().getParameterName(),
            String.format("the required type is - %s", ex.getRequiredType().getSimpleName().toLowerCase()));

      return APIResponse.build(
            HttpStatus.BAD_REQUEST.value(),
            "Validation error",
            errors
      );
   }

   @ExceptionHandler(MissingServletRequestParameterException.class)
   public ResponseEntity<APIResponse<Object>> handleMissingParameter(MissingServletRequestParameterException ex) {
      String errorMessage = "Required parameter '" + ex.getParameterName() + "' is missing.";
      return APIResponse.build(
            HttpStatus.BAD_REQUEST.value(),
            errorMessage,
            null
      );
   }

   @ExceptionHandler(value = {MailSendException.class})
   public ResponseEntity<APIResponse<Object>> handleMailSendException(MailSendException e) {
      return APIResponse.build(
              HttpStatus.SERVICE_UNAVAILABLE.value(),
              "Failed to send email. Please check mail server configuration or try again later.",
              null
      );
   }

   @ExceptionHandler(HttpMessageNotReadableException.class)
   public ResponseEntity<APIResponse<Object>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
      String message = "Invalid request format. Please check your input data.";

      if (ex.getCause() instanceof InvalidFormatException cause && !cause.getPath().isEmpty()) {
         String fieldName = cause.getPath().get(0).getFieldName();
         String expectedType = cause.getTargetType().getSimpleName();
         message = String.format("Invalid value for field '%s': expected type %s", fieldName, expectedType);
      }

      return APIResponse.build(
              HttpStatus.BAD_REQUEST.value(),
              message,
              null
      );
   }

   @ExceptionHandler(Exception.class)
   @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
   public ResponseEntity<APIResponse<Object>> handleAllExceptions(Exception e) {
      logger.error("Unhandled exception", e);
      return APIResponse.build(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "An error occurred while processing your request",
            null
      );
   }

	   private String keycloakClientMessage(KeycloakAuthException e) {
	      logger.info("Keycloak error - Status: {}, Error: {}", e.getStatusCode(), e.getError());
	      if (e.getClientMessage() != null && !e.getClientMessage().isBlank()) {
	         return e.getClientMessage();
	      }

	      if (e.getStatusCode() == HttpStatus.CONFLICT.value()) {
             return "User already exists";
          }

          if (e.getStatusCode() == HttpStatus.UNAUTHORIZED.value()) {
             return "Authentication failed";
          }

      return "Unable to complete request";
   }
}
