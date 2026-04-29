package com.ecommerce.payment.dto;

public record PaymentIntentRequest(
        Long orderId,
        Double amount
) {
}
