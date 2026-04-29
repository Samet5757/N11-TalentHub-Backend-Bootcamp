package com.ecommerce.order.messaging;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface OutboxRepository extends JpaRepository<OutboxMessage, Long> {
    List<OutboxMessage> findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus status);
    List<OutboxMessage> findTop50ByStatusAndNextAttemptAtBeforeOrderByCreatedAtAsc(OutboxStatus status, LocalDateTime nextAttemptAt);
}
