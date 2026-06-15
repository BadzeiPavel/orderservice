package com.innowise.orderservice.model.enums;

public enum Status {
  CREATED,
  CONFIRMED,
  SHIPPED,
  DELIVERED,
  CANCELLED;

  public boolean canTransitionTo(Status next) {
    return switch (this) {
      case CREATED -> next == CONFIRMED || next == CANCELLED;
      case CONFIRMED -> next == SHIPPED || next == CANCELLED;
      case SHIPPED -> next == DELIVERED || next == CANCELLED;
      case DELIVERED, CANCELLED -> false;
    };
  }
}
