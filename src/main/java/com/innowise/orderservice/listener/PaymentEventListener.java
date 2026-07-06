package com.innowise.orderservice.listener;

import com.innowise.commonstarter.config.kafka.KafkaTopics;
import com.innowise.commonstarter.model.dto.event.PaymentCompletedEvent;
import com.innowise.commonstarter.model.dto.event.PaymentCompensationEvent;
import com.innowise.orderservice.exception.OrderNotFoundException;
import com.innowise.orderservice.service.OrderService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventListener {

  private final OrderService orderService;
  private final KafkaTemplate<String, PaymentCompensationEvent> compensationKafkaTemplate;
  private final KafkaTopics kafkaTopics;

  @KafkaListener(topics = "${kafka.topics.payment-events}", groupId = "order-service-group")
  @Retryable(
      retryFor = {Exception.class},
      backoff = @Backoff(delay = 2000, multiplier = 2)
  )
  public void handlePaymentEvent(PaymentCompletedEvent event) {
    log.info("Received payment event: {}", event);
    try {
      orderService.handlePaymentCompletion(event.orderId(), event.status());
      log.info("Order {} updated successfully", event.orderId());
    } catch (OrderNotFoundException e) {
      log.warn("Order {} not found – ignoring event", event.orderId());
    } catch (Exception e) {
      log.error("Failed to process payment event for order {}: {}", event.orderId(), e.getMessage());
      sendCompensation(event.orderId(), e.getMessage());
      throw e;
    }
  }

  private void sendCompensation(UUID orderId, String reason) {
    PaymentCompensationEvent compensation = new PaymentCompensationEvent(orderId, reason);
    compensationKafkaTemplate.send(kafkaTopics.paymentCompensation(), orderId.toString(), compensation);
    log.info("Sent compensation event for order {}", orderId);
  }
}