package com.ecommerce.seller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

public record SellerRequest(
        @NotBlank(message = "storeName is required")
        String storeName,
        @NotBlank(message = "taxNumber is required")
        @Pattern(regexp = "^[0-9]{10,11}$", message = "taxNumber must be 10 or 11 digits")
        String taxNumber,
        @PositiveOrZero(message = "ratingAverage cannot be negative")
        Double ratingAverage,
        @NotNull(message = "isOfficialStore is required")
        Boolean isOfficialStore
) {
}
