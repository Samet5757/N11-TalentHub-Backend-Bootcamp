package com.ecommerce.order.notification;

import com.ecommerce.order.client.AuthServiceClient;
import com.ecommerce.order.client.dto.UserContactDto;
import com.ecommerce.order.entity.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class OrderEmailNotificationService {
    private static final Logger log = LoggerFactory.getLogger(OrderEmailNotificationService.class);

    private final JavaMailSender mailSender;
    private final AuthServiceClient authServiceClient;
    private final String from;
    private final boolean enabled;

    public OrderEmailNotificationService(JavaMailSender mailSender,
                                         AuthServiceClient authServiceClient,
                                         @Value("${spring.mail.username:no-reply@ecommerce.local}") String from,
                                         @Value("${app.notification.order-mail.enabled:false}") boolean enabled) {
        this.mailSender = mailSender;
        this.authServiceClient = authServiceClient;
        this.from = from;
        this.enabled = enabled;
    }

    public void sendOrderCompletedMail(Order order) {
        if (!enabled || order == null || order.getCustomerId() == null || order.getId() == null) {
            return;
        }

        UserContactDto contact = authServiceClient.getUserContact(order.getCustomerId());
        if (contact == null || contact.email() == null || contact.email().isBlank()) {
            throw new IllegalStateException("Customer email is missing for customerId=" + order.getCustomerId());
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(contact.email());
        message.setSubject("Siparisiniz Alindi #" + order.getId());
        message.setText(buildBody(order));
        mailSender.send(message);
        log.info("Order completed mail sent. orderId={}, to={}", order.getId(), contact.email());
    }

    private String buildBody(Order order) {
        return "Merhaba,\n\n"
                + "Siparisiniz basariyla alindi.\n"
                + "Siparis Numaraniz: #" + order.getId() + "\n"
                + "Tutar: " + order.getFinalAmount() + " TRY\n\n"
                + "Tesekkur ederiz.";
    }
}
