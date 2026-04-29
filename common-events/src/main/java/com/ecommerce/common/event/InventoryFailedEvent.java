package com.ecommerce.common.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record InventoryFailedEvent(
        UUID eventId,
        String correlationId,
        LocalDateTime timestamp,
        Long orderId,
        String reason
) {
}
