package com.ecommerce.cart.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record CartRequest(
        @NotNull(message = "customerId is required")
        Long customerId,
        @NotNull(message = "totalAmount is required")
        @PositiveOrZero(message = "totalAmount must be zero or greater")
        Double totalAmount
) {
}
