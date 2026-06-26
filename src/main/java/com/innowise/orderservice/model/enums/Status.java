package com.innowise.orderservice.model.enums;

public enum Status {
  CREATED,
  PAYMENT_FAILED,
  PAID,
  SHIPPED,
  DELIVERED,
  CANCELLED;

  public boolean canTransitionTo(Status next) {
    return switch (this) {
      case CREATED -> next == PAYMENT_FAILED || next == PAID || next == CANCELLED;
      case PAYMENT_FAILED -> next == PAID || next == CANCELLED;
      case PAID -> next == SHIPPED || next == CANCELLED;
      case SHIPPED -> next == DELIVERED || next == CANCELLED;
      case DELIVERED, CANCELLED -> false;
    };
  }
}