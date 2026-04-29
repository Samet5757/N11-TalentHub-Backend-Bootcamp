package com.ecommerce.cart.dto;

public record CartItemResponse(
        Long id,
        Long productId,
        Integer quantity,
        Double unitPrice,
        Double lineTotal
) {
}
