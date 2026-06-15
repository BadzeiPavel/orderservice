package com.innowise.orderservice.service.impl;

import com.innowise.commonstarter.model.dto.UserDto;
import com.innowise.orderservice.exception.OrderNotFoundException;
import com.innowise.orderservice.exception.OrderServiceException;
import com.innowise.orderservice.mapper.OrderMapper;
import com.innowise.orderservice.model.dto.OrderDto;
import com.innowise.orderservice.model.dto.OrderWithUserDto;
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
import com.innowise.orderservice.service.UserServiceGateway;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
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
public class OrderServiceImpl implements OrderService {

  private final OrderRepository orderRepository;
  private final ItemRepository itemRepository;
  private final OrderMapper orderMapper;
  private final UserServiceGateway userServiceGateway;

  @Override
  @Transactional
  public OrderWithUserDto createOrder(OrderCreationDto creationDto) {
    UserDto user = userServiceGateway.fetchUserByEmail(creationDto.email());
    OrderDto orderDto = createOrderInternal(creationDto, user.id());
    return new OrderWithUserDto(orderDto, user);
  }

  @Override
  public OrderWithUserDto getOrderById(UUID id) {
    OrderDto orderDto = orderMapper.toDto(findOrderById(id));
    UserDto user = userServiceGateway.fetchUserById(orderDto.userId());
    return new OrderWithUserDto(orderDto, user);
  }

  @Override
  public Page<OrderWithUserDto> getOrdersFiltered(UUID userId, LocalDateTime startDate,
      LocalDateTime endDate, List<String> statuses, Pageable pageable) {
    List<Status> statusEnums = null;
    if (statuses != null) {
      statusEnums = statuses.stream()
          .map(Status::valueOf)
          .toList();
    }

    Specification<Order> spec = OrderSpecification.withFilters(userId, startDate, endDate,
        statusEnums);
    Page<OrderDto> orderPage = orderRepository.findAll(spec, pageable)
        .map(orderMapper::toDto);

    Map<UUID, UserDto> userCache = new HashMap<>();
    return orderPage.map(dto -> {
      UserDto user = userCache.computeIfAbsent(dto.userId(), userServiceGateway::fetchUserById);
      return new OrderWithUserDto(dto, user);
    });
  }

  @Override
  @Transactional
  public OrderWithUserDto updateOrder(UUID id, OrderUpdateDto updateDto) {
    OrderDto orderDto = updateOrderInternal(id, updateDto);
    UserDto user = userServiceGateway.fetchUserById(orderDto.userId());
    return new OrderWithUserDto(orderDto, user);
  }

  @Override
  @Transactional
  public void deleteOrder(UUID id) {
    int rows = orderRepository.softDeleteById(id);
    if (rows == 0) {
      throw new OrderServiceException("Order not found with id: " + id);
    }
  }

  private OrderDto createOrderInternal(OrderCreationDto creationDto, UUID userId) {
    Order order = Order.builder()
        .userId(userId)
        .status(Status.CREATED)
        .totalPrice(BigDecimal.ZERO)
        .build();
    buildOrderItems(order, creationDto.items());
    order = orderRepository.save(order);
    return orderMapper.toDto(order);
  }

  private OrderDto updateOrderInternal(UUID id, OrderUpdateDto updateDto) {
    Order order = findOrderById(id);

    if (order.getStatus() != updateDto.status() && !order.getStatus()
        .canTransitionTo(updateDto.status())) {
      throw new OrderServiceException(
          "Invalid status transition from " + order.getStatus() + " to " + updateDto.status());
    }

    order.setStatus(updateDto.status());
    if (updateDto.items() != null) {
      order.getOrderItems().clear();
      buildOrderItems(order, updateDto.items());
    }
    order = orderRepository.save(order);
    return orderMapper.toDto(order);
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
    List<OrderItem> orderItems = new ArrayList<>();
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
        .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + id));
  }
}