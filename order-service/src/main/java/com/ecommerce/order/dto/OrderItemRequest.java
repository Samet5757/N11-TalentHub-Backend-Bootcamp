package com.ecommerce.order.dto;

public record OrderItemRequest(
        Long productId,
        Integer quantity,
        Double unitPrice
) {
}
