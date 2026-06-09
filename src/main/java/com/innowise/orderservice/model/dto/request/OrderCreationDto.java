package com.innowise.orderservice.model.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record OrderCreationDto(
    @NotNull
    String email,

    @NotEmpty(message = "Order must contain at least one item")
    List<OrderItemCreationDto> items
) {

}