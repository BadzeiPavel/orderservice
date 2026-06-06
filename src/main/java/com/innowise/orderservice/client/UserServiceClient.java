package com.innowise.orderservice.client;

import com.innowise.commonstarter.model.dto.UserDto;
import com.innowise.orderservice.config.FeignConfig;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "userservice", url = "${service.user-service.url}", configuration = FeignConfig.class)
public interface UserServiceClient {

  @GetMapping("/api/v1/users/{id}")
  ResponseEntity<UserDto> getUserById(@PathVariable UUID id);

  @GetMapping("/api/v1/users")
  ResponseEntity<UserDto> getUserByEmail(@RequestParam String email);
}