package com.ecommerce.product.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class OutboxDispatcher {
    private final OutboxRepository outboxRepository; private final KafkaTemplate<String,Object> kafkaTemplate; private final ObjectMapper objectMapper;
    public OutboxDispatcher(OutboxRepository outboxRepository, KafkaTemplate<String,Object> kafkaTemplate, ObjectMapper objectMapper){this.outboxRepository=outboxRepository;this.kafkaTemplate=kafkaTemplate;this.objectMapper=objectMapper;}
    @Scheduled(fixedDelayString = "${app.outbox.dispatch-interval-ms:1000}")
    @Transactional
    public void dispatch(){
        for(OutboxMessage m: outboxRepository.findTop50ByStatusAndNextAttemptAtBeforeOrderByCreatedAtAsc(OutboxStatus.PENDING, LocalDateTime.now())){
            try{Object p=objectMapper.readValue(m.getPayload(), Class.forName(m.getPayloadType())); kafkaTemplate.send(m.getTopic(),m.getMessageKey(),p); m.setStatus(OutboxStatus.PUBLISHED); m.setPublishedAt(LocalDateTime.now()); m.setErrorMessage(null);}catch(Exception e){m.setStatus(OutboxStatus.FAILED); m.setRetryCount(m.getRetryCount()+1); m.setNextAttemptAt(LocalDateTime.now().plusSeconds(10)); m.setErrorMessage(e.getMessage());}
            outboxRepository.save(m);
        }
    }
}
