package com.ecommerce.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;

public record OrderRequest(
        @NotNull(message = "customerId is required")
        @Positive(message = "customerId must be greater than zero")
        Long customerId,
        @NotNull(message = "sellerId is required")
        @Positive(message = "sellerId must be greater than zero")
        Long sellerId,
        @NotNull(message = "totalAmount is required")
        @PositiveOrZero(message = "totalAmount must be zero or greater")
        Double totalAmount,
        @PositiveOrZero(message = "discountAmount cannot be negative")
        Double discountAmount,
        List<@Valid OrderItemRequest> items
) {
}
