package com.ecommerce.order.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class OrderStatusConstraintFixer {

    private static final Logger log = LoggerFactory.getLogger(OrderStatusConstraintFixer.class);

    @Bean
    ApplicationRunner fixOrderStatusConstraint(JdbcTemplate jdbcTemplate) {
        return args -> {
            try {
                jdbcTemplate.execute("""
                    DO $$
                    BEGIN
                        IF EXISTS (
                            SELECT 1
                            FROM pg_constraint
                            WHERE conname = 'orders_status_check'
                        ) THEN
                            ALTER TABLE orders DROP CONSTRAINT orders_status_check;
                        END IF;

                        ALTER TABLE orders
                        ADD CONSTRAINT orders_status_check
                        CHECK (
                            status IN (
                                'CREATED',
                                'PAID',
                                'APPROVED',
                                'SHIPPED',
                                'DELIVERED',
                                'COMPLETED',
                                'CANCELLED',
                                'PENDING',
                                'INVENTORY_RESERVED',
                                'PAYMENT_PENDING',
                                'PAYMENT_AUTHORIZED',
                                'FAILED'
                            )
                        );
                    END
                    $$;
                    """);
                log.info("orders_status_check constraint reconciled");
            } catch (Exception exception) {
                log.warn("Skipping orders_status_check reconciliation: {}", exception.getMessage());
            }
        };
    }
}
