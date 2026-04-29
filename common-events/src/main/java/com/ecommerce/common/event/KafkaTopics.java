package com.ecommerce.common.event;

public final class KafkaTopics {

    private KafkaTopics() {
    }

    public static final String ORDER_CREATED = "order.created.v1";
    public static final String INVENTORY_RESERVED = "inventory.reserved.v1";
    public static final String INVENTORY_FAILED = "inventory.failed.v1";
    public static final String START_PAYMENT = "payment.start.v1";
    public static final String PAYMENT_AUTHORIZED = "payment.authorized.v1";
    public static final String PAYMENT_FAILED = "payment.failed.v1";
}
