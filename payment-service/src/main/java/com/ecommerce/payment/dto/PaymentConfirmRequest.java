package com.ecommerce.payment.dto;

import jakarta.validation.constraints.NotBlank;

public record PaymentConfirmRequest(
        @NotBlank(message = "cardNumber is required")
        String cardNumber
) {
}
