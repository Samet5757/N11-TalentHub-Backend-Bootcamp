package com.ecommerce.common.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderCreatedEvent(
        UUID eventId,
        String correlationId,
        LocalDateTime timestamp,
        Long orderId,
        Long customerId,
        List<OrderItemPayload> items,
        BigDecimal totalAmount
) {
}
