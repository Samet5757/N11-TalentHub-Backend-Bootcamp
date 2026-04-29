package com.ecommerce.payment.dto;

import com.ecommerce.payment.entity.PaymentStatus;

import java.time.LocalDateTime;

public record PaymentResponse(
        Long id,
        Long orderId,
        Double amount,
        LocalDateTime paymentDate,
        PaymentStatus paymentStatus
) {
}
