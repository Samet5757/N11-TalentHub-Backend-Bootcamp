package com.ecommerce.payment.messaging;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DeadLetterEventRepository extends JpaRepository<DeadLetterEvent, Long> {
}
