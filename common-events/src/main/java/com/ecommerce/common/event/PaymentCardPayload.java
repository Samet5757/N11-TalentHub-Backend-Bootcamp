package com.ecommerce.common.event;

public record PaymentCardPayload(
        String cardToken,
        String cardHolderName,
        String cardLast4,
        String expireMonth,
        String expireYear
) {
}
