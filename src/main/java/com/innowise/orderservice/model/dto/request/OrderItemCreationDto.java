package com.innowise.orderservice.model.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

public record OrderItemCreationDto(
    @NotNull UUID itemId,
    @Positive int quantity
) {

}