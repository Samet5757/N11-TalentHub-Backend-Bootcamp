package com.ecommerce.product.saga;

import com.ecommerce.common.event.OrderCreatedEvent;
import com.ecommerce.product.exception.OutOfStockException;
import com.ecommerce.product.messaging.EventDedupService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Component
public class InventorySagaListener {
    private static final String GROUP = "inventory-participant";

    private final InventoryReservationService inventoryReservationService;
    private final InventorySagaPublisher inventorySagaPublisher;
    private final EventDedupService eventDedupService;

    public InventorySagaListener(InventoryReservationService inventoryReservationService, InventorySagaPublisher inventorySagaPublisher, EventDedupService eventDedupService) {
        this.inventoryReservationService = inventoryReservationService;
        this.inventorySagaPublisher = inventorySagaPublisher;
        this.eventDedupService = eventDedupService;
    }

    @RetryableTopic(attempts = "3", backoff = @Backoff(delay = 2000, multiplier = 2.0), dltTopicSuffix = ".dlq", autoCreateTopics = "true")
    @KafkaListener(topics = com.ecommerce.common.event.KafkaTopics.ORDER_CREATED, groupId = GROUP)
    public void onOrderCreated(OrderCreatedEvent event) {
        if (event == null || event.items() == null || event.items().isEmpty() || eventDedupService.alreadyProcessed(GROUP, event.eventId())) {
            return;
        }
        try {
            inventoryReservationService.reserveStocksAtomically(event);
            inventorySagaPublisher.publishReserved(event.orderId(), event.correlationId(), "Stock reserved");
        } catch (OutOfStockException exception) {
            inventorySagaPublisher.publishFailed(event.orderId(), event.correlationId(), exception.getMessage());
        } catch (RuntimeException exception) {
            inventorySagaPublisher.publishFailed(event.orderId(), event.correlationId(), "Inventory reservation failed");
        }
    }
}
