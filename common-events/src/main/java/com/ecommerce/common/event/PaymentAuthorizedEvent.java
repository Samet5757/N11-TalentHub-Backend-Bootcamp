package com.ecommerce.common.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentAuthorizedEvent(
        UUID eventId,
        String correlationId,
        LocalDateTime timestamp,
        Long orderId,
        Long paymentId
) {
}
