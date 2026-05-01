package com.ecommerce.order.notification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderNotificationOutboxRepository extends JpaRepository<OrderNotificationOutbox, Long> {
    boolean existsByOrderId(Long orderId);

    List<OrderNotificationOutbox> findTop100ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            NotificationStatus status,
            LocalDateTime nextAttemptAt
    );
}
