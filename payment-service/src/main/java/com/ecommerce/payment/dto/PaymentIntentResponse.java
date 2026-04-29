package com.ecommerce.payment.dto;

import com.ecommerce.payment.entity.PaymentStatus;

public record PaymentIntentResponse(
        String paymentIntentId,
        Long orderId,
        Double amount,
        PaymentStatus status
) {
}
