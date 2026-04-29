package com.ecommerce.payment.dto;

public record PaymentRequest(
        Long orderId,
        String cardNumber,
        Double amount
) {
}
