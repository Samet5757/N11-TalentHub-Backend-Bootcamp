package com.ecommerce.cart.dto;

import java.util.List;

public record CartResponse(
        Long id,
        Long customerId,
        Double totalAmount,
        String couponCode,
        Double discountAmount,
        Double finalAmount,
        List<CartItemResponse> items
) {
}
