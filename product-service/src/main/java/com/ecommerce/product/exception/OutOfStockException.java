package com.ecommerce.product.exception;

public class OutOfStockException extends RuntimeException {
    public OutOfStockException(Long productId, Integer quantity) {
        super("Insufficient stock for product: " + productId + ", requested quantity: " + quantity);
    }
}
