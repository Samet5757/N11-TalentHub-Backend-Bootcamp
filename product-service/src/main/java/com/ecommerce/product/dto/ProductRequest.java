package com.ecommerce.product.dto;

import java.math.BigDecimal;

public record ProductRequest(
        String name,
        String description,
        String brand,
        BigDecimal price,
        Integer stock,
        Long categoryId,
        Long sellerId,
        String imageUrl,
        String badgeType,
        Boolean active
) {
}
