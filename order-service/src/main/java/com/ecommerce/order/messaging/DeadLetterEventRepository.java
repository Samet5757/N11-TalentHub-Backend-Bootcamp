package com.ecommerce.order.messaging;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DeadLetterEventRepository extends JpaRepository<DeadLetterEvent, Long> {
}
