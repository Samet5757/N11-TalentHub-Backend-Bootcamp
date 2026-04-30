package com.ecommerce.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PaymentRequest(
        @NotNull(message = "orderId is required")
        @Positive(message = "orderId must be greater than zero")
        Long orderId,
        @NotBlank(message = "cardNumber is required")
        String cardNumber,
        @NotNull(message = "amount is required")
        @Positive(message = "amount must be greater than zero")
        Double amount
) {
}
