package com.ecommerce.order.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrderItemRequest(
        @NotNull(message = "productId is required")
        @Positive(message = "productId must be greater than zero")
        Long productId,
        @NotNull(message = "quantity is required")
        @Positive(message = "quantity must be greater than zero")
        Integer quantity,
        @NotNull(message = "unitPrice is required")
        @Positive(message = "unitPrice must be greater than zero")
        Double unitPrice
) {
}
