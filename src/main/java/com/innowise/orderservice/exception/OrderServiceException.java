package com.innowise.orderservice.exception;

public class OrderServiceException extends RuntimeException {

  public OrderServiceException() {
  }

  public OrderServiceException(String message) {
    super(message);
  }

  public OrderServiceException(String message, Throwable cause) {
    super(message, cause);
  }

  public OrderServiceException(Throwable cause) {
    super(cause);
  }
}
