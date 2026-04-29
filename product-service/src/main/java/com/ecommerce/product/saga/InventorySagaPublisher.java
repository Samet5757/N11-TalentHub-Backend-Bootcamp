package com.ecommerce.product.saga;

import com.ecommerce.common.event.InventoryFailedEvent;
import com.ecommerce.common.event.InventoryReservedEvent;
import com.ecommerce.common.event.KafkaTopics;
import com.ecommerce.product.messaging.OutboxService;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class InventorySagaPublisher {

    private final OutboxService outboxService;

    public InventorySagaPublisher(OutboxService outboxService) {
        this.outboxService = outboxService;
    }

    public void publishReserved(Long orderId, String correlationId, String message) {
        InventoryReservedEvent event = new InventoryReservedEvent(
                UUID.randomUUID(),
                correlationId,
                LocalDateTime.now(),
                orderId,
                true,
                message
        );
        outboxService.enqueue(KafkaTopics.INVENTORY_RESERVED, String.valueOf(orderId), event);
    }

    public void publishFailed(Long orderId, String correlationId, String reason) {
        InventoryFailedEvent event = new InventoryFailedEvent(
                UUID.randomUUID(),
                correlationId,
                LocalDateTime.now(),
                orderId,
                reason
        );
        outboxService.enqueue(KafkaTopics.INVENTORY_FAILED, String.valueOf(orderId), event);
    }
}
