package com.ecommerce.payment.saga;

import com.ecommerce.common.event.StartPaymentEvent;
import com.ecommerce.payment.dto.PaymentRequest;
import com.ecommerce.payment.dto.PaymentResponse;
import com.ecommerce.payment.entity.PaymentStatus;
import com.ecommerce.payment.messaging.EventDedupService;
import com.ecommerce.payment.service.PaymentService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Component
public class PaymentSagaListener {
    private static final String GROUP = "payment-participant";

    private final PaymentService paymentService;
    private final PaymentSagaPublisher paymentSagaPublisher;
    private final EventDedupService eventDedupService;

    public PaymentSagaListener(PaymentService paymentService, PaymentSagaPublisher paymentSagaPublisher, EventDedupService eventDedupService) {
        this.paymentService = paymentService;
        this.paymentSagaPublisher = paymentSagaPublisher;
        this.eventDedupService = eventDedupService;
    }

    @RetryableTopic(attempts = "3", backoff = @Backoff(delay = 2000, multiplier = 2.0), dltTopicSuffix = ".dlq", autoCreateTopics = "true")
    @KafkaListener(topics = com.ecommerce.common.event.KafkaTopics.START_PAYMENT, groupId = GROUP)
    public void onStartPayment(StartPaymentEvent event) {
        if (event == null || event.orderId() == null || event.amount() == null || eventDedupService.alreadyProcessed(GROUP, event.eventId())) {
            return;
        }

        try {
            PaymentRequest request = new PaymentRequest(
                    event.orderId(),
                    "5890040000000016",
                    event.amount().doubleValue()
            );
            PaymentResponse response = paymentService.processPayment(request);

            if (response.paymentStatus() == PaymentStatus.SUCCESS) {
                paymentSagaPublisher.publishAuthorized(event.orderId(), event.correlationId(), response.id());
            } else {
                paymentSagaPublisher.publishFailed(event.orderId(), event.correlationId(), "Payment authorization failed");
            }
        } catch (Exception exception) {
            paymentSagaPublisher.publishFailed(event.orderId(), event.correlationId(), exception.getMessage());
        }
    }
}
