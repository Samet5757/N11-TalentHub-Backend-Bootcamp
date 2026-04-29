package com.ecommerce.order.dto;

import java.util.List;

public record OrderRequest(
        Long customerId,
        Long sellerId,
        Double totalAmount,
        Double discountAmount,
        List<OrderItemRequest> items
) {
}
