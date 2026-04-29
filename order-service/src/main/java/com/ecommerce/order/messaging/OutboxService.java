package com.ecommerce.order.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboxService {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public OutboxService(OutboxRepository outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void enqueue(String topic, String key, Object payload) {
        OutboxMessage message = new OutboxMessage();
        message.setTopic(topic);
        message.setMessageKey(key);
        message.setPayloadType(payload.getClass().getName());
        try {
            message.setPayload(objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize outbox payload", e);
        }
        message.setStatus(OutboxStatus.PENDING);
        outboxRepository.save(message);
    }
}
