package com.ecommerce.order.saga;

import com.ecommerce.common.event.InventoryFailedEvent;
import com.ecommerce.common.event.InventoryReservedEvent;
import com.ecommerce.common.event.KafkaTopics;
import com.ecommerce.common.event.PaymentAuthorizedEvent;
import com.ecommerce.common.event.PaymentFailedEvent;
import com.ecommerce.order.messaging.EventDedupService;
import com.ecommerce.order.service.OrderService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class OrderSagaListener {
    private static final String GROUP = "order-orchestrator";

    private final OrderService orderService;
    private final OrderSagaPublisher orderSagaPublisher;
    private final EventDedupService eventDedupService;

    public OrderSagaListener(OrderService orderService, OrderSagaPublisher orderSagaPublisher, EventDedupService eventDedupService) {
        this.orderService = orderService;
        this.orderSagaPublisher = orderSagaPublisher;
        this.eventDedupService = eventDedupService;
    }

    @RetryableTopic(attempts = "3", backoff = @Backoff(delay = 2000, multiplier = 2.0), dltTopicSuffix = ".dlq", autoCreateTopics = "true")
    @KafkaListener(topics = KafkaTopics.INVENTORY_RESERVED, groupId = GROUP)
    public void onInventoryReserved(InventoryReservedEvent event) {
        if (event == null || !event.success() || eventDedupService.alreadyProcessed(GROUP, event.eventId())) {
            return;
        }
        orderService.updateOrderStatus(event.orderId(), "INVENTORY_RESERVED");
        orderService.updateOrderStatus(event.orderId(), "PAYMENT_PENDING");
        BigDecimal amount = BigDecimal.valueOf(orderService.getOrderById(event.orderId()).finalAmount());
        orderSagaPublisher.publishStartPayment(event.orderId(), amount, event.correlationId());
    }

    @RetryableTopic(attempts = "3", backoff = @Backoff(delay = 2000, multiplier = 2.0), dltTopicSuffix = ".dlq", autoCreateTopics = "true")
    @KafkaListener(topics = KafkaTopics.INVENTORY_FAILED, groupId = GROUP)
    public void onInventoryFailed(InventoryFailedEvent event) {
        if (event == null || eventDedupService.alreadyProcessed(GROUP, event.eventId())) {
            return;
        }
        orderService.updateOrderStatus(event.orderId(), "FAILED");
        orderService.updateOrderStatus(event.orderId(), "CANCELLED");
    }

    @RetryableTopic(attempts = "3", backoff = @Backoff(delay = 2000, multiplier = 2.0), dltTopicSuffix = ".dlq", autoCreateTopics = "true")
    @KafkaListener(topics = KafkaTopics.PAYMENT_AUTHORIZED, groupId = GROUP)
    public void onPaymentAuthorized(PaymentAuthorizedEvent event) {
        if (event == null || eventDedupService.alreadyProcessed(GROUP, event.eventId())) {
            return;
        }
        orderService.updateOrderStatus(event.orderId(), "PAYMENT_AUTHORIZED");
        orderService.updateOrderStatus(event.orderId(), "COMPLETED");
    }

    @RetryableTopic(attempts = "3", backoff = @Backoff(delay = 2000, multiplier = 2.0), dltTopicSuffix = ".dlq", autoCreateTopics = "true")
    @KafkaListener(topics = KafkaTopics.PAYMENT_FAILED, groupId = GROUP)
    public void onPaymentFailed(PaymentFailedEvent event) {
        if (event == null || eventDedupService.alreadyProcessed(GROUP, event.eventId())) {
            return;
        }
        orderService.updateOrderStatus(event.orderId(), "FAILED");
        orderService.updateOrderStatus(event.orderId(), "CANCELLED");
    }
}
