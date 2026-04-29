package com.ecommerce.cart.dto;

public record CartResponse(
        Long id,
        Double totalAmount,
        String couponCode,
        Double discountAmount,
        Double finalAmount
) {
}
