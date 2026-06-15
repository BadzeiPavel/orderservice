package com.innowise.orderservice.service;

import com.innowise.commonstarter.model.dto.UserDto;
import com.innowise.orderservice.client.UserServiceClient;
import com.innowise.orderservice.exception.ServiceUnavailableException;
import com.innowise.orderservice.exception.UserNotFoundException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserServiceGateway {

  private final UserServiceClient userServiceClient;

  @CircuitBreaker(name = "userService", fallbackMethod = "userFallbackByEmail")
  public UserDto fetchUserByEmail(String email) {
    UserDto user = userServiceClient.getUserByEmail(email).getBody();
    if (user == null) {
      throw new UserNotFoundException("User not found with email: " + email);
    }
    return user;
  }

  @CircuitBreaker(name = "userService", fallbackMethod = "userFallbackById")
  public UserDto fetchUserById(UUID userId) {
    UserDto user = userServiceClient.getUserById(userId).getBody();
    if (user == null) {
      throw new UserNotFoundException("User not found with id: " + userId);
    }
    return user;
  }

  private UserDto userFallbackByEmail(String email, Throwable t) {
    throw new ServiceUnavailableException(
        "User service unavailable, cannot find user with email: " + email);
  }

  private UserDto userFallbackById(UUID userId, Throwable t) {
    throw new ServiceUnavailableException(
        "User service unavailable, cannot find user with id: " + userId);
  }
}