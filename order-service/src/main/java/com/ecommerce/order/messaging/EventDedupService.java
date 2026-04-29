package com.ecommerce.order.messaging;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class EventDedupService {

    private final ProcessedEventRepository processedEventRepository;

    public EventDedupService(ProcessedEventRepository processedEventRepository) {
        this.processedEventRepository = processedEventRepository;
    }

    @Transactional
    public boolean alreadyProcessed(String consumerGroup, UUID eventId) {
        if (eventId == null) {
            return false;
        }
        if (processedEventRepository.existsByConsumerGroupAndEventId(consumerGroup, eventId)) {
            return true;
        }
        processedEventRepository.save(new ProcessedEvent(consumerGroup, eventId));
        return false;
    }
}
