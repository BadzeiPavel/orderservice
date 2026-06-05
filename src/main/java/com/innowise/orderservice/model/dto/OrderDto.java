package com.innowise.orderservice.model.dto;

import com.innowise.orderservice.model.dto.response.OrderItemResponseDto;
import com.innowise.orderservice.model.enums.Status;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderDto(
    UUID id,
    UUID userId,
    Status status,
    BigDecimal totalPrice,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    List<OrderItemResponseDto> items
) {

}