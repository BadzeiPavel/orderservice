package com.innowise.orderservice.service.impl.integration;

import com.innowise.commonstarter.model.dto.UserDto;
import com.innowise.orderservice.exception.OrderServiceException;
import com.innowise.orderservice.model.dto.OrderWithUserDto;
import com.innowise.orderservice.model.dto.request.OrderCreationDto;
import com.innowise.orderservice.model.dto.request.OrderItemCreationDto;
import com.innowise.orderservice.model.dto.request.OrderUpdateDto;
import com.innowise.orderservice.model.entity.Item;
import com.innowise.orderservice.model.enums.Status;
import com.innowise.orderservice.repository.ItemRepository;
import com.innowise.orderservice.repository.OrderRepository;
import com.innowise.orderservice.service.OrderService;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderServiceImplIT extends BaseIntegrationTest {

  @Autowired
  private OrderService orderService;

  @Autowired
  private OrderRepository orderRepository;

  @Autowired
  private ItemRepository itemRepository;

  private Item item1;
  private Item item2;
  private final UUID userId = UUID.randomUUID();

  @BeforeEach
  void cleanUp() {
    orderRepository.deleteAll();
    itemRepository.deleteAll();

    item1 = itemRepository.save(
        Item.builder().name("Power bank").price(new BigDecimal("100.00")).build());
    item2 = itemRepository.save(
        Item.builder().name("USB cable").price(new BigDecimal("200.00")).build());
  }

  private void stubUserByEmail(String email, UserDto user) {
    stubFor(get(urlPathEqualTo("/api/v1/users"))
        .withQueryParam("email", equalTo(email))
        .willReturn(aResponse()
            .withStatus(HttpStatus.OK.value())
            .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .withBody(String.format("""
                {
                    "id": "%s",
                    "name": "%s",
                    "surname": "%s",
                    "email": "%s",
                    "active": %b
                }""", user.id(), user.name(), user.surname(), user.email(), user.active()))));
  }

  private void stubUserById(UUID id, UserDto user) {
    stubFor(get(urlPathEqualTo("/api/v1/users/" + id))
        .willReturn(aResponse()
            .withStatus(HttpStatus.OK.value())
            .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .withBody(String.format("""
                {
                    "id": "%s",
                    "name": "%s",
                    "surname": "%s",
                    "email": "%s",
                    "active": %b
                }""", user.id(), user.name(), user.surname(), user.email(), user.active()))));
  }

  @Test
  void createOrder_shouldPersistAndReturnOrderWithUser() {
    UserDto userDto = new UserDto(userId, "John", "Doe", null, "john@example.com", true, null, null,
        null);
    stubUserByEmail("john@example.com", userDto);

    OrderCreationDto dto = new OrderCreationDto("john@example.com",
        List.of(new OrderItemCreationDto(item1.getId(), 2),
            new OrderItemCreationDto(item2.getId(), 1)));

    OrderWithUserDto result = orderService.createOrder(dto);
    assertThat(result.user().id()).isEqualTo(userId);
    assertThat(result.order().items()).hasSize(2);
    assertThat(result.order().totalPrice()).isEqualByComparingTo(new BigDecimal("400.00"));
    assertThat(result.order().status()).isEqualTo(Status.CREATED);
  }

  @Test
  void getOrderById_shouldReturnExistingOrder() {
    UserDto userDto = new UserDto(userId, "John", "Doe", null, "john@example.com", true, null, null,
        null);
    stubUserByEmail("john@example.com", userDto);
    stubUserById(userId, userDto);

    OrderCreationDto dto = new OrderCreationDto("john@example.com",
        List.of(new OrderItemCreationDto(item1.getId(), 1)));
    OrderWithUserDto created = orderService.createOrder(dto);

    OrderWithUserDto fetched = orderService.getOrderById(created.order().id());
    assertThat(fetched.order().id()).isEqualTo(created.order().id());
    assertThat(fetched.user().id()).isEqualTo(userId);
  }

  @Test
  void getOrdersFiltered_shouldFilterByStatus() {
    UserDto userDto = new UserDto(userId, "John", "Doe", null, "john@example.com", true, null, null,
        null);
    stubUserByEmail("john@example.com", userDto);
    stubUserById(userId, userDto);

    orderService.createOrder(new OrderCreationDto("john@example.com",
        List.of(new OrderItemCreationDto(item1.getId(), 1))));

    var page = orderService.getOrdersFiltered(null, null, List.of("CREATED"),
        PageRequest.of(0, 10));
    assertThat(page.getTotalElements()).isEqualTo(1);
  }

  @Test
  void getOrdersByUserId_shouldReturnUserOrders() {
    UserDto userDto = new UserDto(userId, "John", "Doe", null, "john@example.com", true, null, null,
        null);
    stubUserByEmail("john@example.com", userDto);
    stubUserById(userId, userDto);

    orderService.createOrder(new OrderCreationDto("john@example.com",
        List.of(new OrderItemCreationDto(item1.getId(), 1))));

    var page = orderService.getOrdersByUserId(userId, PageRequest.of(0, 10));
    assertThat(page.getTotalElements()).isEqualTo(1);
  }

  @Test
  void updateOrder_shouldChangeStatus() {
    UserDto userDto = new UserDto(userId, "John", "Doe", null, "john@example.com", true, null, null,
        null);
    stubUserByEmail("john@example.com", userDto);
    stubUserById(userId, userDto);

    OrderWithUserDto created = orderService.createOrder(new OrderCreationDto("john@example.com",
        List.of(new OrderItemCreationDto(item1.getId(), 1))));

    OrderUpdateDto update = new OrderUpdateDto(Status.CONFIRMED, null);
    OrderWithUserDto updated = orderService.updateOrder(created.order().id(), update);
    assertThat(updated.order().status()).isEqualTo(Status.CONFIRMED);
  }

  @Test
  void deleteOrder_shouldSoftDelete() {
    UserDto userDto = new UserDto(userId, "John", "Doe", null, "john@example.com", true, null, null,
        null);
    stubUserByEmail("john@example.com", userDto);
    stubUserById(userId, userDto);

    OrderWithUserDto created = orderService.createOrder(new OrderCreationDto("john@example.com",
        List.of(new OrderItemCreationDto(item1.getId(), 1))));

    orderService.deleteOrder(created.order().id());
    UUID orderId = created.order().id();
    assertThatThrownBy(() -> orderService.getOrderById(orderId))
        .isInstanceOf(OrderServiceException.class);
  }
}