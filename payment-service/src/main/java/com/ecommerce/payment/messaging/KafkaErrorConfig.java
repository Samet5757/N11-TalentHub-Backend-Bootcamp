package com.ecommerce.payment.messaging;

import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaErrorConfig {
    @Bean
    public DefaultErrorHandler defaultErrorHandler(KafkaOperations<Object,Object> kafkaOperations){
        DeadLetterPublishingRecoverer recoverer=new DeadLetterPublishingRecoverer(kafkaOperations,(record,exception)->new TopicPartition(record.topic()+".dlq",record.partition()));
        return new DefaultErrorHandler(recoverer,new FixedBackOff(2000L,2L));
    }
}
