package com.ecommerce.seller.dto;

public record SellerRequest(
        String storeName,
        String taxNumber,
        Double ratingAverage,
        Boolean isOfficialStore
) {
}
