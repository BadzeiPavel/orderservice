package com.innowise.orderservice.mapper;

import com.innowise.orderservice.model.dto.OrderDto;
import com.innowise.orderservice.model.dto.response.OrderItemResponseDto;
import com.innowise.orderservice.model.entity.Order;
import com.innowise.orderservice.model.entity.OrderItem;
import java.math.BigDecimal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderMapper {

  @Mapping(target = "items", source = "orderItems")
  OrderDto toDto(Order order);

  default OrderItemResponseDto mapOrderItem(OrderItem orderItem) {
    return new OrderItemResponseDto(
        orderItem.getItem().getId(),
        orderItem.getItem().getName(),
        orderItem.getItem().getPrice(),
        orderItem.getQuantity(),
        orderItem.getItem().getPrice().multiply(BigDecimal.valueOf(orderItem.getQuantity()))
    );
  }
}