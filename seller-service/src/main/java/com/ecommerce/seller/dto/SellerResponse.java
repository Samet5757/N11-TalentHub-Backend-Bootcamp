package com.ecommerce.seller.dto;

public record SellerResponse(
        Long id,
        String storeName,
        String taxNumber,
        Double ratingAverage,
        Boolean isOfficialStore
) {
}
