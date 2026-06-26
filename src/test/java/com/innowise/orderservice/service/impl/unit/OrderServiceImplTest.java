package com.innowise.orderservice.service.impl.unit;

import com.innowise.commonstarter.model.dto.UserDto;
import com.innowise.commonstarter.model.enums.PaymentStatus;
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
import com.innowise.orderservice.service.UserServiceGateway;
import com.innowise.orderservice.service.impl.OrderServiceImpl;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

  @Mock
  private OrderRepository orderRepository;
  @Mock
  private ItemRepository itemRepository;
  @Mock
  private OrderMapper orderMapper;
  @Mock
  private UserServiceGateway userServiceGateway;

  @InjectMocks
  private OrderServiceImpl orderService;

  private final UUID orderId = UUID.randomUUID();
  private final UUID userId = UUID.randomUUID();
  private final UUID itemId1 = UUID.randomUUID();
  private final UUID itemId2 = UUID.randomUUID();
  private final Item item1 = Item.builder().id(itemId1).name("Item 1")
      .price(new BigDecimal("100.00")).build();
  private final Item item2 = Item.builder().id(itemId2).name("Item 2")
      .price(new BigDecimal("200.00")).build();
  private final UserDto userDto = new UserDto(userId, "John", "Doe", null, "john@example.com", true,
      null, null, null);

  private Order buildOrder(Status status) {
    Order order = Order.builder()
        .id(orderId)
        .userId(userId)
        .status(status)
        .totalPrice(new BigDecimal("300.00"))
        .deleted(false)
        .build();
    order.setOrderItems(new ArrayList<>());
    return order;
  }

  @Test
  void createOrder_shouldSucceed() {
    OrderCreationDto creationDto = new OrderCreationDto("john@example.com",
        List.of(new OrderItemCreationDto(itemId1, 2), new OrderItemCreationDto(itemId2, 1)));

    when(userServiceGateway.fetchUserByEmail("john@example.com")).thenReturn(userDto);
    when(itemRepository.findAllById(Set.of(itemId1, itemId2))).thenReturn(List.of(item1, item2));
    when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
    OrderDto orderDtoMock = mock(OrderDto.class);
    when(orderMapper.toDto(any(Order.class))).thenReturn(orderDtoMock);

    OrderWithUserDto result = orderService.createOrder(creationDto);
    assertThat(result.user()).isEqualTo(userDto);
    verify(orderRepository).save(any(Order.class));
  }

  @Test
  void createOrder_shouldThrowWhenUserNotFound() {
    OrderCreationDto dto = new OrderCreationDto("unknown@example.com",
        List.of(new OrderItemCreationDto(itemId1, 1)));
    when(userServiceGateway.fetchUserByEmail("unknown@example.com"))
        .thenThrow(new OrderServiceException("User not found"));

    assertThatThrownBy(() -> orderService.createOrder(dto))
        .isInstanceOf(OrderServiceException.class)
        .hasMessageContaining("User not found");
  }

  @Test
  void createOrder_shouldThrowWhenItemsEmpty() {
    when(userServiceGateway.fetchUserByEmail("john@example.com")).thenReturn(userDto);
    OrderCreationDto dto = new OrderCreationDto("john@example.com", List.of());
    assertThatThrownBy(() -> orderService.createOrder(dto))
        .isInstanceOf(OrderServiceException.class)
        .hasMessageContaining("at least one item");
  }

  @Test
  void getOrderById_shouldReturnOrderWithUser() {
    Order order = buildOrder(Status.CREATED);
    when(orderRepository.findByIdAndDeletedFalse(orderId)).thenReturn(Optional.of(order));
    OrderDto orderDtoMock = mock(OrderDto.class);
    when(orderDtoMock.userId()).thenReturn(userId);
    when(orderMapper.toDto(order)).thenReturn(orderDtoMock);
    when(userServiceGateway.fetchUserById(userId)).thenReturn(userDto);

    OrderWithUserDto result = orderService.getOrderById(orderId);
    assertThat(result.user()).isEqualTo(userDto);
  }

  @Test
  void getOrderById_shouldThrowWhenNotFound() {
    when(orderRepository.findByIdAndDeletedFalse(orderId)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> orderService.getOrderById(orderId))
        .isInstanceOf(OrderNotFoundException.class);
  }

  @Test
  void getOrdersFiltered_shouldReturnPage() {
    Order order = buildOrder(Status.CREATED);
    Page<Order> page = new PageImpl<>(List.of(order));
    when(orderRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(
        page);
    OrderDto orderDtoMock = mock(OrderDto.class);
    when(orderDtoMock.userId()).thenReturn(userId);
    when(orderMapper.toDto(order)).thenReturn(orderDtoMock);
    when(userServiceGateway.fetchUserById(userId)).thenReturn(userDto);

    Page<OrderWithUserDto> result = orderService.getOrdersFiltered(null, null, null,
        List.of("CREATED"), PageRequest.of(0, 10));
    assertThat(result.getTotalElements()).isEqualTo(1);
  }

  @Test
  void handlePaymentCompletion_shouldUpdateOrderToPaymentFailed_whenPaymentFails() {
    // Given
    Order order = new Order();
    order.setId(orderId);
    order.setStatus(Status.CREATED); // or any status that can transition to PAYMENT_FAILED
    when(orderRepository.findByIdAndDeletedFalse(orderId)).thenReturn(Optional.of(order));

    // When
    orderService.handlePaymentCompletion(orderId, PaymentStatus.FAILED);

    // Then
    assertThat(order.getStatus()).isEqualTo(Status.PAYMENT_FAILED);
    verify(orderRepository).save(order);
    // The log line will be executed
  }

  @Test
  void handlePaymentCompletion_shouldUpdateOrderToPaid_whenPaymentSucceeds() {
    // Given
    Order order = new Order();
    order.setId(orderId);
    order.setStatus(Status.CREATED);
    when(orderRepository.findByIdAndDeletedFalse(orderId)).thenReturn(Optional.of(order));

    // When
    orderService.handlePaymentCompletion(orderId, PaymentStatus.SUCCESS);

    // Then
    assertThat(order.getStatus()).isEqualTo(Status.PAID);
    verify(orderRepository).save(order);
  }

  @Test
  void updateOrder_shouldChangeStatus() {
    Order order = buildOrder(Status.CREATED);
    OrderUpdateDto updateDto = new OrderUpdateDto(Status.PAID, null);
    when(orderRepository.findByIdAndDeletedFalse(orderId)).thenReturn(Optional.of(order));
    when(orderRepository.save(any(Order.class))).thenReturn(order);
    OrderDto orderDtoMock = mock(OrderDto.class);
    when(orderDtoMock.userId()).thenReturn(userId);
    when(orderMapper.toDto(any(Order.class))).thenReturn(orderDtoMock);
    when(userServiceGateway.fetchUserById(userId)).thenReturn(userDto);

    OrderWithUserDto result = orderService.updateOrder(orderId, updateDto);
    assertThat(result.user()).isEqualTo(userDto);
  }

  @Test
  void updateOrder_shouldThrowWhenInvalidStatusTransition() {
    Order order = buildOrder(Status.DELIVERED);
    OrderUpdateDto updateDto = new OrderUpdateDto(Status.CREATED, null);
    when(orderRepository.findByIdAndDeletedFalse(orderId)).thenReturn(Optional.of(order));

    assertThatThrownBy(() -> orderService.updateOrder(orderId, updateDto))
        .isInstanceOf(OrderServiceException.class)
        .hasMessageContaining("Invalid status transition");
  }

  @Test
  void updateOrder_shouldReplaceItemsAndRecalculateTotal() {
    Order order = buildOrder(Status.CREATED);
    order.setOrderItems(new ArrayList<>(List.of(
        OrderItem.builder().order(order).item(item1).quantity(1).build()
    )));
    order.setTotalPrice(new BigDecimal("100.00"));

    UUID newItemId = UUID.randomUUID();
    Item newItem = Item.builder().id(newItemId).name("New").price(new BigDecimal("50.00")).build();

    OrderUpdateDto updateDto = new OrderUpdateDto(Status.PAID,
        List.of(new OrderItemCreationDto(newItemId, 3)));

    when(orderRepository.findByIdAndDeletedFalse(orderId)).thenReturn(Optional.of(order));
    when(itemRepository.findAllById(Set.of(newItemId))).thenReturn(List.of(newItem));
    when(orderRepository.save(any(Order.class))).thenReturn(order);
    OrderDto orderDtoMock = mock(OrderDto.class);
    when(orderDtoMock.userId()).thenReturn(userId);
    when(orderMapper.toDto(any(Order.class))).thenReturn(orderDtoMock);
    when(userServiceGateway.fetchUserById(userId)).thenReturn(userDto);

    OrderWithUserDto result = orderService.updateOrder(orderId, updateDto);
    verify(orderRepository).save(order);
    assertThat(order.getTotalPrice()).isEqualByComparingTo(new BigDecimal("150.00"));
  }

  @Test
  void deleteOrder_shouldSoftDelete() {
    when(orderRepository.softDeleteById(orderId)).thenReturn(1);
    assertThatCode(() -> orderService.deleteOrder(orderId)).doesNotThrowAnyException();
  }

  @Test
  void deleteOrder_shouldThrowWhenNotFound() {
    when(orderRepository.softDeleteById(orderId)).thenReturn(0);
    assertThatThrownBy(() -> orderService.deleteOrder(orderId))
        .isInstanceOf(OrderServiceException.class);
  }
}