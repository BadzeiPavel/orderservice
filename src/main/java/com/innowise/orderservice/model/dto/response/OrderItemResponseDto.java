package com.innowise.orderservice.model.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponseDto(
    UUID itemId,
    String itemName,
    BigDecimal itemPrice,
    int quantity,
    BigDecimal totalPrice
) {

}