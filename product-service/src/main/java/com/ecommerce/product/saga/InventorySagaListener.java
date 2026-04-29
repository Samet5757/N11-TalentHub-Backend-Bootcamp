package com.ecommerce.product.saga;

import com.ecommerce.common.event.OrderCreatedEvent;
import com.ecommerce.common.event.OrderItemPayload;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.messaging.EventDedupService;
import com.ecommerce.product.repository.ProductRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
public class InventorySagaListener {
    private static final String GROUP = "inventory-participant";

    private final ProductRepository productRepository;
    private final InventorySagaPublisher inventorySagaPublisher;
    private final EventDedupService eventDedupService;

    public InventorySagaListener(ProductRepository productRepository, InventorySagaPublisher inventorySagaPublisher, EventDedupService eventDedupService) {
        this.productRepository = productRepository;
        this.inventorySagaPublisher = inventorySagaPublisher;
        this.eventDedupService = eventDedupService;
    }

    @Transactional
    @RetryableTopic(attempts = "3", backoff = @Backoff(delay = 2000, multiplier = 2.0), dltTopicSuffix = ".dlq", autoCreateTopics = "true")
    @KafkaListener(topics = com.ecommerce.common.event.KafkaTopics.ORDER_CREATED, groupId = GROUP)
    public void onOrderCreated(OrderCreatedEvent event) {
        if (event == null || event.items() == null || event.items().isEmpty() || eventDedupService.alreadyProcessed(GROUP, event.eventId())) {
            return;
        }

        for (OrderItemPayload item : event.items()) {
            Optional<Product> productOptional = productRepository.findById(item.productId());
            if (productOptional.isEmpty()) {
                inventorySagaPublisher.publishFailed(event.orderId(), event.correlationId(), "Product not found: " + item.productId());
                return;
            }
            Product product = productOptional.get();
            if (product.getStock() < item.quantity()) {
                inventorySagaPublisher.publishFailed(event.orderId(), event.correlationId(), "Insufficient stock for product: " + item.productId());
                return;
            }
        }

        for (OrderItemPayload item : event.items()) {
            Product product = productRepository.findById(item.productId()).orElseThrow();
            product.setStock(product.getStock() - item.quantity());
            productRepository.save(product);
        }

        inventorySagaPublisher.publishReserved(event.orderId(), event.correlationId(), "Stock reserved");
    }
}
