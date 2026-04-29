package com.ecommerce.payment.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class PaymentStatusConstraintFixer {

    private static final Logger log = LoggerFactory.getLogger(PaymentStatusConstraintFixer.class);

    @Bean
    ApplicationRunner fixPaymentStatusConstraint(JdbcTemplate jdbcTemplate) {
        return args -> {
            try {
                jdbcTemplate.execute("""
                    DO $$
                    BEGIN
                        IF EXISTS (
                            SELECT 1
                            FROM pg_constraint
                            WHERE conname = 'payments_payment_status_check'
                        ) THEN
                            ALTER TABLE payments DROP CONSTRAINT payments_payment_status_check;
                        END IF;

                        ALTER TABLE payments
                        ADD CONSTRAINT payments_payment_status_check
                        CHECK (payment_status IN ('PENDING', 'SUCCESS', 'FAILED'));
                    END
                    $$;
                    """);
                log.info("payments_payment_status_check constraint reconciled");
            } catch (Exception exception) {
                log.warn("Skipping payments_payment_status_check reconciliation: {}", exception.getMessage());
            }
        };
    }
}
