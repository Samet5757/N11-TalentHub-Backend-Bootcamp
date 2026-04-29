package com.ecommerce.campaign.dto;

import com.ecommerce.campaign.entity.DiscountType;

public record CampaignResponse(
        Long id,
        String code,
        DiscountType discountType,
        Double discountValue,
        boolean active
) {
}
