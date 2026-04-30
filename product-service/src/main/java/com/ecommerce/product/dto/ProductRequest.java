package com.ecommerce.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank(message = "name is required")
        String name,
        String description,
        String brand,
        @NotNull(message = "price is required")
        @Positive(message = "price must be greater than zero")
        BigDecimal price,
        @NotNull(message = "stock is required")
        @PositiveOrZero(message = "stock cannot be negative")
        Integer stock,
        @NotNull(message = "categoryId is required")
        @Positive(message = "categoryId must be greater than zero")
        Long categoryId,
        @NotNull(message = "sellerId is required")
        @Positive(message = "sellerId must be greater than zero")
        Long sellerId,
        String imageUrl,
        String badgeType,
        Boolean active
) {
}
