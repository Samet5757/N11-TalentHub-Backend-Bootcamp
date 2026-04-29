package com.ecommerce.order.dto;

public record OrderItemResponse(
        Long id,
        Long orderId,
        Long productId,
        Integer quantity,
        Double unitPrice
) {
}
