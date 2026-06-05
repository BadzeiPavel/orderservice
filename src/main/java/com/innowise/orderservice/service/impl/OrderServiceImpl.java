package com.innowise.orderservice.service.impl;

import com.innowise.orderservice.exception.OrderServiceException;
import com.innowise.orderservice.mapper.OrderMapper;
import com.innowise.orderservice.model.dto.OrderDto;
import com.innowise.orderservice.model.dto.request.OrderCreationDto;
import com.innowise.orderservice.model.dto.request.OrderItemCreationDto;
import com.innowise.orderservice.model.dto.request.OrderUpdateDto;
import com.innowise.orderservice.model.entity.Item;
import com.innowise.orderservice.model.entity.Order;
import com.innowise.orderservice.model.entity.OrderItem;
import com.innowise.orderservice.model.enums.Status;
import com.innowise.orderservice.repository.ItemRepository;
import com.innowise.orderservice.repository.OrderRepository;
import com.innowise.orderservice.repository.specification.OrderSpecification;
import com.innowise.orderservice.service.OrderService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderServiceImpl implements OrderService {

  private final OrderRepository orderRepository;
  private final ItemRepository itemRepository;
  private final OrderMapper orderMapper;

  @Override
  public OrderDto createOrder(OrderCreationDto creationDto) {
    Order order = Order.builder()
        .userId(creationDto.userId())
        .status(Status.CREATED)
        .totalPrice(BigDecimal.ZERO)
        .build();

    buildOrderItems(order, creationDto.items());

    order = orderRepository.save(order);
    return orderMapper.toDto(order);
  }

  @Override
  @Transactional(readOnly = true)
  public OrderDto getOrderById(UUID id) {
    return orderMapper.toDto(findOrderById(id));
  }

  @Override
  @Transactional(readOnly = true)
  public Page<OrderDto> getOrdersFiltered(LocalDateTime startDate, LocalDateTime endDate,
      List<String> statuses, Pageable pageable) {
    Specification<Order> spec = OrderSpecification.withFilters(startDate, endDate, statuses);
    return orderRepository.findAllOrders(spec, pageable).map(orderMapper::toDto);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<OrderDto> getOrdersByUserId(UUID userId, Pageable pageable) {
    return orderRepository.findByUserIdAndDeletedFalse(userId, pageable)
        .map(orderMapper::toDto);
  }

  @Override
  public OrderDto updateOrder(UUID id, OrderUpdateDto updateDto) {
    Order order = findOrderById(id);
    order.setStatus(updateDto.status());

    if (updateDto.items() != null) {
      order.getOrderItems().clear();

      buildOrderItems(order, updateDto.items());
    }

    order = orderRepository.save(order);
    return orderMapper.toDto(order);
  }

  @Override
  public void deleteOrder(UUID id) {
    int rows = orderRepository.softDeleteById(id);
    if (rows == 0) {
      throw new OrderServiceException("Order not found with id: " + id);
    }
  }

  private void buildOrderItems(Order order, List<OrderItemCreationDto> itemDtos) {
    Set<UUID> uniqueIds = validateItemDtos(itemDtos);
    Map<UUID, Item> itemMap = fetchItemsByIds(uniqueIds);
    populateItemsAndTotal(order, itemDtos, itemMap);
  }

  private Set<UUID> validateItemDtos(List<OrderItemCreationDto> itemDtos) {
    if (itemDtos.isEmpty()) {
      throw new OrderServiceException("Order must contain at least one item");
    }
    Set<UUID> uniqueIds = new HashSet<>();
    for (OrderItemCreationDto dto : itemDtos) {
      if (!uniqueIds.add(dto.itemId())) {
        throw new OrderServiceException("Duplicate item IDs are not allowed: " + dto.itemId());
      }
    }
    return uniqueIds;
  }

  private Map<UUID, Item> fetchItemsByIds(Set<UUID> itemIds) {
    List<Item> items = itemRepository.findAllById(itemIds);
    if (items.size() != itemIds.size()) {
      throw new OrderServiceException("Some items not found");
    }
    return items.stream().collect(Collectors.toMap(Item::getId, i -> i));
  }

  private void populateItemsAndTotal(Order order, List<OrderItemCreationDto> itemDtos,
      Map<UUID, Item> itemMap) {
    Set<OrderItem> orderItems = new HashSet<>();
    BigDecimal total = BigDecimal.ZERO;

    for (OrderItemCreationDto dto : itemDtos) {
      Item item = itemMap.get(dto.itemId());
      OrderItem orderItem = OrderItem.builder()
          .order(order)
          .item(item)
          .quantity(dto.quantity())
          .build();
      orderItems.add(orderItem);
      total = total.add(item.getPrice().multiply(BigDecimal.valueOf(dto.quantity())));
    }
    order.setOrderItems(orderItems);
    order.setTotalPrice(total);
  }

  private Order findOrderById(UUID id) {
    return orderRepository.findByIdAndDeletedFalse(id)
        .orElseThrow(() -> new OrderServiceException("Order not found with id: " + id));
  }
}