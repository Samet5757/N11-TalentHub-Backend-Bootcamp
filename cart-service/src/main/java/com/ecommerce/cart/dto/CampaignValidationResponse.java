package com.ecommerce.cart.dto;

public record CampaignValidationResponse(
        Long id,
        String code,
        String discountType,
        Double discountValue,
        boolean active
) {
}
