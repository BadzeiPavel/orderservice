package com.innowise.orderservice.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.innowise.commonstarter.model.dto.response.ErrorResponse;
import com.innowise.orderservice.exception.OrderNotFoundException;
import com.innowise.orderservice.exception.OrderServiceException;
import com.innowise.orderservice.exception.ServiceUnavailableException;
import com.innowise.orderservice.exception.UserNotFoundException;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(OrderNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleNotFound(OrderNotFoundException ex,
      HttpServletRequest request) {
    return buildResponse("Not Found", ex.getMessage(), ex.getClass().getSimpleName(),
        HttpStatus.NOT_FOUND, request);
  }

  @ExceptionHandler(UserNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex,
      HttpServletRequest request) {
    return buildResponse("Not Found", ex.getMessage(), ex.getClass().getSimpleName(),
        HttpStatus.NOT_FOUND, request);
  }

  @ExceptionHandler(ServiceUnavailableException.class)
  public ResponseEntity<ErrorResponse> handleServiceUnavailable(ServiceUnavailableException ex,
      HttpServletRequest request) {
    return buildResponse("Service Unavailable", ex.getMessage(), ex.getClass().getSimpleName(),
        HttpStatus.SERVICE_UNAVAILABLE, request);
  }

  @ExceptionHandler(OrderServiceException.class)
  public ResponseEntity<ErrorResponse> handleOrderService(OrderServiceException ex,
      HttpServletRequest request) {
    return buildResponse("Order error", ex.getMessage(), ex.getClass().getSimpleName(),
        HttpStatus.BAD_REQUEST, request);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
      HttpServletRequest request) {
    String errors = ex.getBindingResult().getFieldErrors().stream()
        .map(e -> e.getField() + ": " + e.getDefaultMessage())
        .collect(Collectors.joining("; "));
    return buildResponse("Validation Failed", errors, ex.getClass().getSimpleName(),
        HttpStatus.BAD_REQUEST, request);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex,
      HttpServletRequest request) {
    return buildResponse("Bad Request", ex.getMessage(), ex.getClass().getSimpleName(),
        HttpStatus.BAD_REQUEST, request);
  }

  @ExceptionHandler(FeignException.class)
  public ResponseEntity<ErrorResponse> handleFeign(FeignException ex, HttpServletRequest request) {
    HttpStatus status = switch (ex.status()) {
      case 400 -> HttpStatus.BAD_REQUEST;
      case 404 -> HttpStatus.NOT_FOUND;
      case 409 -> HttpStatus.CONFLICT;
      default -> HttpStatus.BAD_GATEWAY;
    };
    String message = extractFeignMessage(ex);
    return buildResponse("Downstream error", message, ex.getClass().getSimpleName(), status,
        request);
  }

  @ExceptionHandler(CallNotPermittedException.class)
  public ResponseEntity<ErrorResponse> handleCircuitBreaker(CallNotPermittedException ex,
      HttpServletRequest request) {
    return buildResponse("Service Unavailable",
        "User service is temporarily unavailable, please try again later",
        ex.getClass().getSimpleName(),
        HttpStatus.SERVICE_UNAVAILABLE, request);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
    return buildResponse("Internal Server Error",
        "An unexpected error occurred",
        ex.getClass().getSimpleName(),
        HttpStatus.INTERNAL_SERVER_ERROR, request);
  }

  private ResponseEntity<ErrorResponse> buildResponse(String title, String message,
      String exceptionName,
      HttpStatus status,
      HttpServletRequest request) {
    ErrorResponse response = ErrorResponse.builder()
        .title(title)
        .name(exceptionName)
        .status(status.value())
        .message(message)
        .path(request.getRequestURI())
        .timestamp(LocalDateTime.now())
        .build();
    return new ResponseEntity<>(response, status);
  }

  private String extractFeignMessage(FeignException ex) {
    try {
      String body = ex.contentUTF8();
      if (body != null && !body.isEmpty()) {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(body);
        if (root.has("message")) {
          return root.get("message").asText();
        }
      }
    } catch (Exception ignored) {
      return "Failed to parse json response";
    }
    return ex.getMessage();
  }
}