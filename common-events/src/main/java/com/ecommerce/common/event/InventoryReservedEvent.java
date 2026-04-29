package com.ecommerce.common.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record InventoryReservedEvent(
        UUID eventId,
        String correlationId,
        LocalDateTime timestamp,
        Long orderId,
        boolean success,
        String message
) {
}
