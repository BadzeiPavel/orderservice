package com.innowise.orderservice.service.impl.integration;

import com.innowise.orderservice.model.dto.request.OrderCreationDto;
import com.innowise.orderservice.model.dto.request.OrderItemCreationDto;
import com.innowise.orderservice.model.dto.request.OrderUpdateDto;
import com.innowise.orderservice.model.enums.Status;

import java.util.List;
import java.util.UUID;

public class TestDataFactory {

    public static OrderCreationDto createOrderCreationDto(String email, List<OrderItemCreationDto> items) {
        return new OrderCreationDto(email, items);
    }

    public static OrderItemCreationDto createOrderItemDto(UUID itemId, int quantity) {
        return new OrderItemCreationDto(itemId, quantity);
    }

    public static OrderUpdateDto createOrderUpdateDto(Status status) {
        return new OrderUpdateDto(status, null);
    }
}