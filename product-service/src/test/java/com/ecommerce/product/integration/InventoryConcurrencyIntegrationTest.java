package com.ecommerce.product.integration;

import com.ecommerce.common.event.KafkaTopics;
import com.ecommerce.common.event.OrderCreatedEvent;
import com.ecommerce.common.event.OrderItemPayload;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.mapper.ProductMapper;
import com.ecommerce.product.messaging.EventDedupService;
import com.ecommerce.product.messaging.OutboxRepository;
import com.ecommerce.product.messaging.OutboxService;
import com.ecommerce.product.messaging.ProcessedEventRepository;
import com.ecommerce.product.repository.ProductRepository;
import com.ecommerce.product.saga.InventorySagaListener;
import com.ecommerce.product.saga.InventorySagaPublisher;
import com.ecommerce.product.saga.InventoryReservationService;
import com.ecommerce.product.service.ProductService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.liquibase.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import({
        ProductService.class,
        ProductMapper.class,
        OutboxService.class,
        EventDedupService.class,
        InventoryReservationService.class,
        InventorySagaPublisher.class,
        InventorySagaListener.class,
        InventoryConcurrencyIntegrationTest.TestConfig.class
})
class InventoryConcurrencyIntegrationTest {

    @Autowired
    private InventorySagaListener inventorySagaListener;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    private Long productId;

    @TestConfiguration
    static class TestConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper().findAndRegisterModules();
        }
    }

    @AfterEach
    void tearDown() {
        outboxRepository.deleteAll();
        processedEventRepository.deleteAll();
        productRepository.deleteAll();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void oneStock_fiveParallelOrders_onlyOneShouldReserve() throws Exception {
        Product product = new Product();
        product.setName("iPhone Tekli Stok");
        product.setDescription("Concurrency testi");
        product.setBrand("Apple");
        product.setPrice(new BigDecimal("99999.00"));
        product.setStock(1);
        product.setCategoryId(1L);
        product.setSellerId(1L);
        product.setActive(true);
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        productId = productRepository.saveAndFlush(product).getId();

        ExecutorService executor = Executors.newFixedThreadPool(5);
        try {
            List<CompletableFuture<Void>> futures = java.util.stream.IntStream.range(0, 5)
                    .mapToObj(i -> CompletableFuture.runAsync(() -> {
                        OrderCreatedEvent event = new OrderCreatedEvent(
                                UUID.randomUUID(),
                                "corr-" + i,
                                LocalDateTime.now(),
                                10_000L + i,
                                100L + i,
                                List.of(new OrderItemPayload(productId, 1, new BigDecimal("99999.00"))),
                                new BigDecimal("99999.00")
                        );
                        inventorySagaListener.onOrderCreated(event);
                    }, executor))
                    .toList();

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } finally {
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        }

        Product latest = productRepository.findById(productId).orElseThrow();
        long reservedCount = outboxRepository.findAll().stream()
                .filter(message -> KafkaTopics.INVENTORY_RESERVED.equals(message.getTopic()))
                .count();
        long failedCount = outboxRepository.findAll().stream()
                .filter(message -> KafkaTopics.INVENTORY_FAILED.equals(message.getTopic()))
                .count();

        assertThat(latest.getStock()).isZero();
        assertThat(reservedCount).isEqualTo(1);
        assertThat(failedCount).isEqualTo(4);
    }
}
