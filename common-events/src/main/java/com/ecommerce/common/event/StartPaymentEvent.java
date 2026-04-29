package com.ecommerce.common.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record StartPaymentEvent(
        UUID eventId,
        String correlationId,
        LocalDateTime timestamp,
        Long orderId,
        BigDecimal amount,
        PaymentCardPayload paymentCard
) {
}
