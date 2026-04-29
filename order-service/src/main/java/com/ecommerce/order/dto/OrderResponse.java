package com.ecommerce.order.dto;

import com.ecommerce.order.entity.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        Long customerId,
        Long sellerId,
        Double totalAmount,
        Double discountAmount,
        Double finalAmount,
        OrderStatus status,
        LocalDateTime createdAt,
        List<OrderItemResponse> items
) {
}
