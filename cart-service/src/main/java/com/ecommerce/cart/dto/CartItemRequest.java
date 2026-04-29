package com.ecommerce.cart.dto;

public record CartItemRequest(
        Long productId,
        Integer quantity,
        Double unitPrice
) {
}
