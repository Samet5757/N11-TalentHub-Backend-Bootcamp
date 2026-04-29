package com.ecommerce.order.saga;

import com.ecommerce.common.event.KafkaTopics;
import com.ecommerce.common.event.OrderCreatedEvent;
import com.ecommerce.common.event.OrderItemPayload;
import com.ecommerce.order.messaging.OutboxService;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
public class OrderSagaPublisher {

    private final OutboxService outboxService;

    public OrderSagaPublisher(OutboxService outboxService) {
        this.outboxService = outboxService;
    }

    public void publishOrderCreated(Order order) {
        List<OrderItemPayload> items = order.getItems().stream()
                .map(this::toPayload)
                .toList();

        OrderCreatedEvent event = new OrderCreatedEvent(
                UUID.randomUUID(),
                "order-" + order.getId(),
                LocalDateTime.now(),
                order.getId(),
                order.getCustomerId(),
                items,
                BigDecimal.valueOf(order.getFinalAmount())
        );

        outboxService.enqueue(KafkaTopics.ORDER_CREATED, String.valueOf(order.getId()), event);
    }

    public void publishStartPayment(Long orderId, BigDecimal amount, String correlationId) {
        outboxService.enqueue(KafkaTopics.START_PAYMENT, String.valueOf(orderId), new com.ecommerce.common.event.StartPaymentEvent(
                UUID.randomUUID(),
                correlationId,
                LocalDateTime.now(),
                orderId,
                amount,
                new com.ecommerce.common.event.PaymentCardPayload(null, null, null, null, null)
        ));
    }

    private OrderItemPayload toPayload(OrderItem item) {
        return new OrderItemPayload(
                item.getProductId(),
                item.getQuantity(),
                BigDecimal.valueOf(item.getUnitPrice())
        );
    }
}
