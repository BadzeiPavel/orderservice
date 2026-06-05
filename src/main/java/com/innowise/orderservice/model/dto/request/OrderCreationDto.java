package com.innowise.orderservice.model.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record OrderCreationDto(
    @NotNull
    UUID userId,

    @NotEmpty(message = "Order must contain at least one item")
    List<OrderItemCreationDto> items
) {

}