package com.ecommerce.seller.exception;

public class SellerNotFoundException extends RuntimeException {

    public SellerNotFoundException(Long sellerId) {
        super("Seller not found with id: " + sellerId);
    }
}
