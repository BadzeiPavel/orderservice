package com.innowise.orderservice.listener;

import com.innowise.commonstarter.config.kafka.KafkaTopics;
import com.innowise.commonstarter.model.dto.event.PaymentCompletedEvent;
import com.innowise.commonstarter.model.dto.event.PaymentCompensationEvent;
import com.innowise.orderservice.service.OrderService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventListener {

  private final OrderService orderService;
  private final KafkaTemplate<String, PaymentCompensationEvent> compensationKafkaTemplate;
  private final KafkaTopics kafkaTopics;

  @KafkaListener(topics = "${kafka.topics.payment-events}", groupId = "order-service-group")
  public void handlePaymentEvent(PaymentCompletedEvent event) {
    log.info("Received payment event: {}", event);
    try {
      orderService.handlePaymentCompletion(event.orderId(), event.status());
      log.info("Order {} updated successfully", event.orderId());
    } catch (Exception e) {
      log.error("Failed to process payment event for order {}: {}", event.orderId(), e.getMessage());
      sendCompensation(event.orderId(), e.getMessage());
    }
  }

  private void sendCompensation(UUID orderId, String reason) {
    PaymentCompensationEvent compensation = new PaymentCompensationEvent(orderId, reason);
    compensationKafkaTemplate.send(kafkaTopics.paymentCompensation(), orderId.toString(), compensation);
    log.info("Sent compensation event for order {}", orderId);
  }
}