package com.ecommerce.order.notification;

import com.ecommerce.order.entity.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class OrderNotificationOutboxService {

    private final OrderNotificationOutboxRepository outboxRepository;

    public OrderNotificationOutboxService(OrderNotificationOutboxRepository outboxRepository) {
        this.outboxRepository = outboxRepository;
    }

    @Transactional
    public void enqueueCompletedOrderMail(Order order) {
        if (order == null || order.getId() == null || order.getCustomerId() == null) {
            return;
        }
        if (outboxRepository.existsByOrderId(order.getId())) {
            return;
        }

        OrderNotificationOutbox entry = new OrderNotificationOutbox();
        entry.setOrderId(order.getId());
        entry.setCustomerId(order.getCustomerId());
        entry.setStatus(NotificationStatus.PENDING);
        entry.setRetryCount(0);
        entry.setMaxRetry(10);
        entry.setCreatedAt(LocalDateTime.now());
        entry.setNextAttemptAt(LocalDateTime.now());
        outboxRepository.save(entry);
    }
}
