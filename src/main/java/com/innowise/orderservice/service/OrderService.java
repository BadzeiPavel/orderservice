package com.innowise.orderservice.service;

import com.innowise.orderservice.model.dto.OrderDto;
import com.innowise.orderservice.model.dto.request.OrderCreationDto;
import com.innowise.orderservice.model.dto.request.OrderUpdateDto;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderService {

  OrderDto createOrder(OrderCreationDto creationDto);

  OrderDto getOrderById(UUID id);

  Page<OrderDto> getOrdersFiltered(LocalDateTime startDate, LocalDateTime endDate,
      List<String> statuses, Pageable pageable);

  Page<OrderDto> getOrdersByUserId(UUID userId, Pageable pageable);

  OrderDto updateOrder(UUID id, OrderUpdateDto updateDto);

  void deleteOrder(UUID id);
}