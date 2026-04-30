package com.ecommerce.order.saga;

import com.ecommerce.common.event.InventoryFailedEvent;
import com.ecommerce.common.event.PaymentFailedEvent;
import com.ecommerce.order.messaging.EventDedupService;
import com.ecommerce.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderSagaListenerTest {

    @Mock
    private OrderService orderService;

    @Mock
    private EventDedupService eventDedupService;

    @InjectMocks
    private OrderSagaListener orderSagaListener;

    @Test
    void onInventoryFailed_shouldMarkOrderAsFailed() {
        UUID eventId = UUID.randomUUID();
        when(eventDedupService.alreadyProcessed("order-orchestrator", eventId)).thenReturn(false);

        orderSagaListener.onInventoryFailed(new InventoryFailedEvent(
                eventId,
                "corr-1",
                LocalDateTime.now(),
                101L,
                "Insufficient stock"
        ));

        verify(orderService).updateOrderStatus(101L, "FAILED");
    }

    @Test
    void onPaymentFailed_shouldTriggerCompensationStatusFlow() {
        UUID eventId = UUID.randomUUID();
        when(eventDedupService.alreadyProcessed("order-orchestrator", eventId)).thenReturn(false);

        orderSagaListener.onPaymentFailed(new PaymentFailedEvent(
                eventId,
                "corr-2",
                LocalDateTime.now(),
                202L,
                "Payment provider rejected"
        ));

        verify(orderService).updateOrderStatus(202L, "FAILED");
        verify(orderService).updateOrderStatus(202L, "CANCELLED");
    }

    @Test
    void onInventoryFailed_shouldIgnoreDuplicateEvent() {
        UUID eventId = UUID.randomUUID();
        when(eventDedupService.alreadyProcessed("order-orchestrator", eventId)).thenReturn(true);

        orderSagaListener.onInventoryFailed(new InventoryFailedEvent(
                eventId,
                "corr-3",
                LocalDateTime.now(),
                303L,
                "Duplicate event"
        ));

        verify(orderService, never()).updateOrderStatus(303L, "FAILED");
    }
}
