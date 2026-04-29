package com.ecommerce.order.messaging;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class DlqListener {

    private final DeadLetterEventRepository deadLetterEventRepository;

    public DlqListener(DeadLetterEventRepository deadLetterEventRepository){
        this.deadLetterEventRepository = deadLetterEventRepository;
    }

    @KafkaListener(topicPattern = ".*\\.dlq", groupId = "order-dlq-consumer")
    public void onDlq(ConsumerRecord<String, Object> record){
        DeadLetterEvent e = new DeadLetterEvent();
        e.setTopic(record.topic());
        e.setMessageKey(record.key() == null ? "" : record.key());
        e.setPayload(record.value() == null ? "" : String.valueOf(record.value()));
        deadLetterEventRepository.save(e);
    }
}
