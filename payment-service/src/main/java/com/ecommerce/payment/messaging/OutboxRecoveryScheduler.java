package com.ecommerce.payment.messaging;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class OutboxRecoveryScheduler {
    private final OutboxRepository outboxRepository;
    public OutboxRecoveryScheduler(OutboxRepository outboxRepository){this.outboxRepository=outboxRepository;}

    @Scheduled(fixedDelayString = "${app.outbox.recovery-interval-ms:5000}")
    @Transactional
    public void requeueFailedMessages(){
        for(OutboxMessage m: outboxRepository.findTop50ByStatusAndNextAttemptAtBeforeOrderByCreatedAtAsc(OutboxStatus.FAILED, LocalDateTime.now())){
            if(m.getRetryCount()>=m.getMaxRetry()) continue;
            m.setStatus(OutboxStatus.PENDING);
            outboxRepository.save(m);
        }
    }
}
