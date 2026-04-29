package com.ecommerce.order.messaging;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class OutboxRecoveryScheduler {

    private final OutboxRepository outboxRepository;

    public OutboxRecoveryScheduler(OutboxRepository outboxRepository) {
        this.outboxRepository = outboxRepository;
    }

    @Scheduled(fixedDelayString = "${app.outbox.recovery-interval-ms:5000}")
    @Transactional
    public void requeueFailedMessages() {
        List<OutboxMessage> failed = outboxRepository.findTop50ByStatusAndNextAttemptAtBeforeOrderByCreatedAtAsc(
                OutboxStatus.FAILED,
                LocalDateTime.now()
        );

        for (OutboxMessage message : failed) {
            if (message.getRetryCount() >= message.getMaxRetry()) {
                continue;
            }
            message.setStatus(OutboxStatus.PENDING);
            outboxRepository.save(message);
        }
    }
}
