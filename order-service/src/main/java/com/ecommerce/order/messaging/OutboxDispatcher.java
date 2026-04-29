package com.ecommerce.order.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class OutboxDispatcher {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public OutboxDispatcher(OutboxRepository outboxRepository, KafkaTemplate<String, Object> kafkaTemplate, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${app.outbox.dispatch-interval-ms:1000}")
    @Transactional
    public void dispatch() {
        List<OutboxMessage> messages = outboxRepository.findTop50ByStatusAndNextAttemptAtBeforeOrderByCreatedAtAsc(
                OutboxStatus.PENDING, LocalDateTime.now());
        for (OutboxMessage message : messages) {
            try {
                Class<?> payloadClass = Class.forName(message.getPayloadType());
                Object payload = objectMapper.readValue(message.getPayload(), payloadClass);
                kafkaTemplate.send(message.getTopic(), message.getMessageKey(), payload);
                message.setStatus(OutboxStatus.PUBLISHED);
                message.setPublishedAt(LocalDateTime.now());
                message.setErrorMessage(null);
            } catch (Exception exception) {
                message.setStatus(OutboxStatus.FAILED);
                message.setRetryCount(message.getRetryCount() + 1);
                message.setNextAttemptAt(LocalDateTime.now().plusSeconds(10));
                message.setErrorMessage(exception.getMessage());
            }
            outboxRepository.save(message);
        }
    }
}
