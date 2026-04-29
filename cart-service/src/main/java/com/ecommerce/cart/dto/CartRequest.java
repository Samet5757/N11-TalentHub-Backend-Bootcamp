package com.ecommerce.cart.dto;

public record CartRequest(
        Long customerId,
        Double totalAmount
) {
}
