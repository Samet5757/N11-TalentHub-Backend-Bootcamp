package com.ecommerce.order.notification;

import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderStatus;
import com.ecommerce.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class OrderNotificationDispatcher {
    private static final Logger log = LoggerFactory.getLogger(OrderNotificationDispatcher.class);

    private final OrderNotificationOutboxRepository outboxRepository;
    private final OrderRepository orderRepository;
    private final OrderEmailNotificationService orderEmailNotificationService;

    public OrderNotificationDispatcher(OrderNotificationOutboxRepository outboxRepository,
                                       OrderRepository orderRepository,
                                       OrderEmailNotificationService orderEmailNotificationService) {
        this.outboxRepository = outboxRepository;
        this.orderRepository = orderRepository;
        this.orderEmailNotificationService = orderEmailNotificationService;
    }

    @Scheduled(fixedDelayString = "${app.notification.order-mail.dispatch-delay-ms:3000}")
    @Transactional
    public void dispatch() {
        List<OrderNotificationOutbox> due = outboxRepository
                .findTop100ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(NotificationStatus.PENDING, LocalDateTime.now());

        for (OrderNotificationOutbox entry : due) {
            try {
                Order order = orderRepository.findById(entry.getOrderId()).orElse(null);
                if (order == null || order.getStatus() != OrderStatus.COMPLETED) {
                    entry.setStatus(NotificationStatus.FAILED);
                    entry.setErrorMessage("Order not found or not completed");
                    continue;
                }

                orderEmailNotificationService.sendOrderCompletedMail(order);
                entry.setStatus(NotificationStatus.SENT);
                entry.setSentAt(LocalDateTime.now());
                entry.setErrorMessage(null);
            } catch (Exception exception) {
                int nextRetryCount = entry.getRetryCount() + 1;
                entry.setRetryCount(nextRetryCount);
                entry.setErrorMessage(exception.getMessage());

                if (nextRetryCount >= entry.getMaxRetry()) {
                    entry.setStatus(NotificationStatus.FAILED);
                    log.error("Order notification exhausted retries. orderId={}, retries={}", entry.getOrderId(), nextRetryCount);
                } else {
                    entry.setNextAttemptAt(LocalDateTime.now().plusSeconds(Math.min(60, nextRetryCount * 2L)));
                    log.warn("Order notification send failed. orderId={}, retry={}", entry.getOrderId(), nextRetryCount, exception);
                }
            }
        }
    }
}
