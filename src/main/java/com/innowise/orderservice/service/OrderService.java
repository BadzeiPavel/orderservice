package com.innowise.orderservice.service;

import com.innowise.orderservice.model.dto.OrderWithUserDto;
import com.innowise.orderservice.model.dto.request.OrderCreationDto;
import com.innowise.orderservice.model.dto.request.OrderUpdateDto;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderService {

  OrderWithUserDto createOrder(OrderCreationDto creationDto);

  OrderWithUserDto getOrderById(UUID id);

  Page<OrderWithUserDto> getOrdersFiltered(LocalDateTime startDate, LocalDateTime endDate,
      List<String> statuses, Pageable pageable);

  Page<OrderWithUserDto> getOrdersByUserId(UUID userId, Pageable pageable);

  OrderWithUserDto updateOrder(UUID id, OrderUpdateDto updateDto);

  void deleteOrder(UUID id);
}