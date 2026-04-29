package com.ecommerce.order.messaging;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "processed_events", uniqueConstraints = {
        @UniqueConstraint(name = "uk_processed_event", columnNames = {"consumer_group", "event_id"})
})
public class ProcessedEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "consumer_group", nullable = false)
    private String consumerGroup;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(nullable = false)
    private LocalDateTime processedAt;

    public ProcessedEvent() {
    }

    public ProcessedEvent(String consumerGroup, UUID eventId) {
        this.consumerGroup = consumerGroup;
        this.eventId = eventId;
        this.processedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
}
