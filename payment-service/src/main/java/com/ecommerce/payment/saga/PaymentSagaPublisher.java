package com.ecommerce.payment.saga;

import com.ecommerce.common.event.KafkaTopics;
import com.ecommerce.common.event.PaymentAuthorizedEvent;
import com.ecommerce.common.event.PaymentFailedEvent;
import com.ecommerce.payment.messaging.OutboxService;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class PaymentSagaPublisher {

    private final OutboxService outboxService;

    public PaymentSagaPublisher(OutboxService outboxService) {
        this.outboxService = outboxService;
    }

    public void publishAuthorized(Long orderId, String correlationId, Long paymentId) {
        PaymentAuthorizedEvent event = new PaymentAuthorizedEvent(
                UUID.randomUUID(),
                correlationId,
                LocalDateTime.now(),
                orderId,
                paymentId
        );
        outboxService.enqueue(KafkaTopics.PAYMENT_AUTHORIZED, String.valueOf(orderId), event);
    }

    public void publishFailed(Long orderId, String correlationId, String reason) {
        PaymentFailedEvent event = new PaymentFailedEvent(
                UUID.randomUUID(),
                correlationId,
                LocalDateTime.now(),
                orderId,
                reason
        );
        outboxService.enqueue(KafkaTopics.PAYMENT_FAILED, String.valueOf(orderId), event);
    }
}
