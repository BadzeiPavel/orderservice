package com.innowise.orderservice.exception;

public class OrderNotFoundException extends RuntimeException {

  public OrderNotFoundException(String message) {
    super(message);
  }

  public OrderNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }

  public OrderNotFoundException(Throwable cause) {
    super(cause);
  }

  public OrderNotFoundException() {
  }
}