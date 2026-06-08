package com.innowise.orderservice.service.impl.unit;

import com.innowise.commonstarter.model.dto.UserDto;
import com.innowise.orderservice.client.UserServiceClient;
import com.innowise.orderservice.exception.OrderServiceException;
import com.innowise.orderservice.mapper.OrderMapper;
import com.innowise.orderservice.model.dto.OrderDto;
import com.innowise.orderservice.model.dto.OrderWithUserDto;
import com.innowise.orderservice.model.dto.request.OrderCreationDto;
import com.innowise.orderservice.model.dto.request.OrderItemCreationDto;
import com.innowise.orderservice.model.dto.request.OrderUpdateDto;
import com.innowise.orderservice.model.entity.Item;
import com.innowise.orderservice.model.entity.Order;
import com.innowise.orderservice.model.enums.Status;
import com.innowise.orderservice.repository.ItemRepository;
import com.innowise.orderservice.repository.OrderRepository;
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
import org.springframework.http.ResponseEntity;

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
  private UserServiceClient userServiceClient;

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

  @Test
  void createOrder_shouldSucceed() {
    OrderCreationDto creationDto = new OrderCreationDto("john@example.com",
        List.of(new OrderItemCreationDto(itemId1, 2),
                new OrderItemCreationDto(itemId2, 1)));

    when(userServiceClient.getUserByEmail("john@example.com"))
        .thenReturn(ResponseEntity.ok(userDto));
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
    when(userServiceClient.getUserByEmail("unknown@example.com"))
        .thenReturn(ResponseEntity.ok(null));

    assertThatThrownBy(() -> orderService.createOrder(dto))
        .isInstanceOf(OrderServiceException.class)
        .hasMessageContaining("User not found");
  }

  @Test
  void createOrder_shouldThrowWhenItemsEmpty() {
    when(userServiceClient.getUserByEmail("john@example.com"))
        .thenReturn(ResponseEntity.ok(userDto));

    OrderCreationDto dto = new OrderCreationDto("john@example.com", List.of());
    assertThatThrownBy(() -> orderService.createOrder(dto))
        .isInstanceOf(OrderServiceException.class)
        .hasMessageContaining("at least one item");
  }

  @Test
  void getOrderById_shouldReturnOrderWithUser() {
    Order order = buildOrder();
    when(orderRepository.findByIdAndDeletedFalse(orderId)).thenReturn(Optional.of(order));
    OrderDto orderDtoMock = mock(OrderDto.class);
    when(orderDtoMock.userId()).thenReturn(userId);
    when(orderMapper.toDto(order)).thenReturn(orderDtoMock);
    when(userServiceClient.getUserById(userId)).thenReturn(ResponseEntity.ok(userDto));

    OrderWithUserDto result = orderService.getOrderById(orderId);
    assertThat(result.user()).isEqualTo(userDto);
  }

  @Test
  void getOrderById_shouldThrowWhenNotFound() {
    when(orderRepository.findByIdAndDeletedFalse(orderId)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> orderService.getOrderById(orderId))
        .isInstanceOf(OrderServiceException.class);
  }

  @Test
  void getOrdersFiltered_shouldReturnPage() {
    Order order = buildOrder();
    Page<Order> page = new PageImpl<>(List.of(order));
    when(orderRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(
        page);
    OrderDto orderDtoMock = mock(OrderDto.class);
    when(orderDtoMock.userId()).thenReturn(userId);
    when(orderMapper.toDto(order)).thenReturn(orderDtoMock);
    when(userServiceClient.getUserById(userId)).thenReturn(ResponseEntity.ok(userDto));

    Page<OrderWithUserDto> result = orderService.getOrdersFiltered(null, null, null,
        PageRequest.of(0, 10));
    assertThat(result.getTotalElements()).isEqualTo(1);
  }

  @Test
  void updateOrder_shouldChangeStatus() {
    Order order = buildOrder();
    OrderUpdateDto updateDto = new OrderUpdateDto(Status.CONFIRMED, null);
    when(orderRepository.findByIdAndDeletedFalse(orderId)).thenReturn(Optional.of(order));
    when(orderRepository.save(any(Order.class))).thenReturn(order);
    OrderDto orderDtoMock = mock(OrderDto.class);
    when(orderDtoMock.userId()).thenReturn(userId);
    when(orderMapper.toDto(any(Order.class))).thenReturn(orderDtoMock);
    when(userServiceClient.getUserById(userId)).thenReturn(ResponseEntity.ok(userDto));

    OrderWithUserDto result = orderService.updateOrder(orderId, updateDto);
    assertThat(result.user()).isEqualTo(userDto);
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

  private Order buildOrder() {
    Order order = Order.builder()
        .id(orderId)
        .userId(userId)
        .status(Status.CREATED)
        .totalPrice(new BigDecimal("300.00"))
        .deleted(false)
        .build();
    order.setOrderItems(new ArrayList<>());
    return order;
  }
}