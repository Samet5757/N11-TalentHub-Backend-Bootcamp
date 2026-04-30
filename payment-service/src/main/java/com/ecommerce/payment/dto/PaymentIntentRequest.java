package com.ecommerce.payment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PaymentIntentRequest(
        @NotNull(message = "orderId is required")
        @Positive(message = "orderId must be greater than zero")
        Long orderId,
        @NotNull(message = "amount is required")
        @Positive(message = "amount must be greater than zero")
        Double amount
) {
}
