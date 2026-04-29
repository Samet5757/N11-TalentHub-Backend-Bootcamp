package com.ecommerce.payment.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboxService {
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    public OutboxService(OutboxRepository outboxRepository, ObjectMapper objectMapper){this.outboxRepository=outboxRepository;this.objectMapper=objectMapper;}
    @Transactional
    public void enqueue(String topic, String key, Object payload){
        OutboxMessage m=new OutboxMessage();
        m.setTopic(topic);m.setMessageKey(key);m.setPayloadType(payload.getClass().getName());
        try{m.setPayload(objectMapper.writeValueAsString(payload));}catch(JsonProcessingException e){throw new IllegalStateException(e);}
        m.setStatus(OutboxStatus.PENDING);
        outboxRepository.save(m);
    }
}
