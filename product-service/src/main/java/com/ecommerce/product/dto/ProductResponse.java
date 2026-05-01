package com.ecommerce.product.dto;

import java.math.BigDecimal;
import java.io.Serializable;
import java.time.LocalDateTime;

public record ProductResponse(
        Long id,
        String name,
        String description,
        String brand,
        BigDecimal price,
        Integer stock,
        Long categoryId,
        Long sellerId,
        String imageUrl,
        Double ratingAverage,
        Integer reviewCount,
        Boolean active,
        String badgeType,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) implements Serializable {
}
