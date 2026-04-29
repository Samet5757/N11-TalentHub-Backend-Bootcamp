package com.ecommerce.common.event;

import java.math.BigDecimal;

public record OrderItemPayload(
        Long productId,
        Integer quantity,
        BigDecimal unitPrice
) {
}
