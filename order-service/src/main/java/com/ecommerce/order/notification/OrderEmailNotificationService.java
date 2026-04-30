package com.ecommerce.order.notification;

import com.ecommerce.order.entity.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class OrderEmailNotificationService {
    private static final Logger log = LoggerFactory.getLogger(OrderEmailNotificationService.class);

    private final JavaMailSender mailSender;
    private final String from;
    private final String recipientTemplate;
    private final boolean enabled;

    public OrderEmailNotificationService(JavaMailSender mailSender,
                                         @Value("${spring.mail.username:no-reply@ecommerce.local}") String from,
                                         @Value("${app.notification.order-mail.recipient-template:customer%d@example.local}") String recipientTemplate,
                                         @Value("${app.notification.order-mail.enabled:false}") boolean enabled) {
        this.mailSender = mailSender;
        this.from = from;
        this.recipientTemplate = recipientTemplate;
        this.enabled = enabled;
    }

    @Async
    public void sendOrderCompletedMail(Order order) {
        if (!enabled || order == null || order.getCustomerId() == null || order.getId() == null) {
            return;
        }
        String to = String.format(recipientTemplate, order.getCustomerId());
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject("Siparisiniz Alindi #" + order.getId());
        message.setText(buildBody(order));
        try {
            mailSender.send(message);
            log.info("Order completed mail sent. orderId={}, to={}", order.getId(), to);
        } catch (Exception exception) {
            log.error("Failed to send order completed mail. orderId={}, to={}", order.getId(), to, exception);
        }
    }

    private String buildBody(Order order) {
        return "Merhaba,\n\n"
                + "Siparisiniz basariyla alindi.\n"
                + "Siparis Numaraniz: #" + order.getId() + "\n"
                + "Tutar: " + order.getFinalAmount() + " TRY\n\n"
                + "Tesekkur ederiz.";
    }
}
