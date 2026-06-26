package com.innowise.orderservice.service.impl.integration;

import com.innowise.commonstarter.model.dto.UserDto;
import com.innowise.orderservice.exception.ServiceUnavailableException;
import com.innowise.orderservice.model.dto.OrderWithUserDto;
import com.innowise.orderservice.model.dto.request.OrderCreationDto;
import com.innowise.orderservice.model.dto.request.OrderUpdateDto;
import com.innowise.orderservice.model.entity.Item;
import com.innowise.orderservice.model.enums.Status;
import com.innowise.orderservice.repository.ItemRepository;
import com.innowise.orderservice.repository.OrderRepository;
import com.innowise.orderservice.service.UserServiceGateway;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class OrderControllerIT extends BaseControllerIT {

  @Autowired
  private OrderRepository orderRepository;
  @Autowired
  private ItemRepository itemRepository;

  @MockitoBean
  private UserServiceGateway userServiceGateway;

  private Item item1, item2;
  private final UUID userId = UUID.randomUUID();
  private final String email = "john@example.com";

  @BeforeEach
  void cleanUp() {
    orderRepository.deleteAll();
    itemRepository.deleteAll();
    item1 = itemRepository.save(
        Item.builder().name("Power bank").price(new BigDecimal("100.00")).build());
    item2 = itemRepository.save(
        Item.builder().name("USB cable").price(new BigDecimal("200.00")).build());

    UserDto userDto = new UserDto(userId, "John", "Doe", null, email, true, null, null, null);

    when(userServiceGateway.fetchUserByEmail(email)).thenReturn(userDto);
    when(userServiceGateway.fetchUserById(any(UUID.class))).thenReturn(userDto);
  }

  @Test
  void createOrder_shouldReturn201() {
    OrderCreationDto dto = TestDataFactory.createOrderCreationDto(email,
        List.of(TestDataFactory.createOrderItemDto(item1.getId(), 2),
            TestDataFactory.createOrderItemDto(item2.getId(), 1)));

    ResponseEntity<OrderWithUserDto> resp = restTemplate.postForEntity(
        baseUrl() + "/api/v1/orders", dto, OrderWithUserDto.class);
    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    OrderWithUserDto body = resp.getBody();
    assertThat(body).isNotNull();
    assertThat(body.order().userId()).isEqualTo(userId);
    assertThat(body.order().items()).hasSize(2);
    assertThat(body.order().totalPrice()).isEqualByComparingTo(new BigDecimal("400.00"));
    assertThat(body.order().status()).isEqualTo(Status.CREATED);
  }

  @Test
  void getOrderById_shouldReturn200() {
    OrderCreationDto dto = TestDataFactory.createOrderCreationDto(email,
        List.of(TestDataFactory.createOrderItemDto(item1.getId(), 1)));
    ResponseEntity<OrderWithUserDto> created = restTemplate.postForEntity(
        baseUrl() + "/api/v1/orders", dto, OrderWithUserDto.class);
    Assertions.assertNotNull(created.getBody());
    UUID orderId = created.getBody().order().id();

    ResponseEntity<OrderWithUserDto> resp = restTemplate.getForEntity(
        baseUrl() + "/api/v1/orders/" + orderId, OrderWithUserDto.class);
    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(resp.getBody()).isNotNull();
    assertThat(resp.getBody().order().id()).isEqualTo(orderId);
    assertThat(resp.getBody().user().id()).isEqualTo(userId);
  }

  @Test
  void getOrdersFiltered_shouldReturnPage() {
    OrderCreationDto dto = TestDataFactory.createOrderCreationDto(email,
        List.of(TestDataFactory.createOrderItemDto(item1.getId(), 1)));
    restTemplate.postForEntity(baseUrl() + "/api/v1/orders", dto, OrderWithUserDto.class);

    ResponseEntity<String> resp = restTemplate.exchange(
        baseUrl() + "/api/v1/orders?statuses=CREATED&page=0&size=10",
        HttpMethod.GET, null, String.class);
    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    DocumentContext json = JsonPath.parse(resp.getBody());
    assertThat(json.read("content.length()", Integer.class)).isEqualTo(1);
  }

  @Test
  void getOrdersByUserId_viaFilterParameter_shouldReturnPage() {
    OrderCreationDto dto = TestDataFactory.createOrderCreationDto(email,
        List.of(TestDataFactory.createOrderItemDto(item1.getId(), 1)));
    restTemplate.postForEntity(baseUrl() + "/api/v1/orders", dto, OrderWithUserDto.class);

    ResponseEntity<String> resp = restTemplate.exchange(
        baseUrl() + "/api/v1/orders?userId=" + userId + "&page=0&size=10",
        HttpMethod.GET, null, String.class);
    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    DocumentContext json = JsonPath.parse(resp.getBody());
    assertThat(json.read("content.length()", Integer.class)).isEqualTo(1);
  }

  @Test
  void updateOrder_shouldReturn200() {
    OrderCreationDto dto = TestDataFactory.createOrderCreationDto(email,
        List.of(TestDataFactory.createOrderItemDto(item1.getId(), 1)));
    ResponseEntity<OrderWithUserDto> created = restTemplate.postForEntity(
        baseUrl() + "/api/v1/orders", dto, OrderWithUserDto.class);
    Assertions.assertNotNull(created.getBody());
    UUID orderId = created.getBody().order().id();

    OrderUpdateDto updateDto = TestDataFactory.createOrderUpdateDto(Status.PAID);
    HttpEntity<OrderUpdateDto> request = createEntity(updateDto);
    ResponseEntity<OrderWithUserDto> resp = restTemplate.exchange(
        baseUrl() + "/api/v1/orders/" + orderId, HttpMethod.PATCH, request,
        OrderWithUserDto.class);
    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(resp.getBody()).isNotNull();
    assertThat(resp.getBody().order().status()).isEqualTo(Status.PAID);
  }

  @Test
  void updateOrder_whenUserServiceFails_shouldReturn503() {
    when(userServiceGateway.fetchUserById(any(UUID.class)))
        .thenThrow(new ServiceUnavailableException("User service unavailable"));

    OrderCreationDto createDto = TestDataFactory.createOrderCreationDto(email,
        List.of(TestDataFactory.createOrderItemDto(item1.getId(), 1)));
    ResponseEntity<OrderWithUserDto> created = restTemplate.postForEntity(
        baseUrl() + "/api/v1/orders", createDto, OrderWithUserDto.class);
    Assertions.assertNotNull(created.getBody());
    UUID orderId = created.getBody().order().id();

    OrderUpdateDto updateDto = new OrderUpdateDto(Status.PAID, null);
    HttpEntity<OrderUpdateDto> request = createEntity(updateDto);
    ResponseEntity<String> resp = restTemplate.exchange(
        baseUrl() + "/api/v1/orders/" + orderId, HttpMethod.PATCH, request, String.class);

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
  }

  @Test
  void deleteOrder_shouldReturn204() {
    OrderCreationDto dto = TestDataFactory.createOrderCreationDto(email,
        List.of(TestDataFactory.createOrderItemDto(item1.getId(), 1)));
    ResponseEntity<OrderWithUserDto> created = restTemplate.postForEntity(
        baseUrl() + "/api/v1/orders", dto, OrderWithUserDto.class);
    Assertions.assertNotNull(created.getBody());
    UUID orderId = created.getBody().order().id();

    ResponseEntity<Void> deleteResp = restTemplate.exchange(
        baseUrl() + "/api/v1/orders/" + orderId, HttpMethod.DELETE, null, Void.class);
    assertThat(deleteResp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    ResponseEntity<String> getResp = restTemplate.exchange(
        baseUrl() + "/api/v1/orders/" + orderId, HttpMethod.GET, null, String.class);
    assertThat(getResp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }
}