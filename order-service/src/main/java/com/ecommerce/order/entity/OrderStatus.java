package com.ecommerce.order.entity;

public enum OrderStatus {
    PENDING,
    INVENTORY_RESERVED,
    PAYMENT_PENDING,
    PAYMENT_AUTHORIZED,
    APPROVED,
    SHIPPED,
    DELIVERED,
    COMPLETED,
    FAILED,
    CANCELLED
}
