package com.innowise.orderservice.model.dto.request;

import com.innowise.orderservice.model.enums.Status;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record OrderUpdateDto(
    @NotNull Status status,
    List<OrderItemCreationDto> items
) {

}