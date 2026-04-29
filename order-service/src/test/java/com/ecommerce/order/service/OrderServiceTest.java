package com.ecommerce.order.service;

import com.ecommerce.order.dto.OrderItemRequest;
import com.ecommerce.order.dto.OrderRequest;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderStatus;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.order.saga.OrderSagaPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderSagaPublisher orderSagaPublisher;

    @InjectMocks
    private OrderService orderService;

    @Test
    void createOrder_shouldCalculateFinalAmountAndPublishEvent() {
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(99L);
            return order;
        });

        OrderRequest request = new OrderRequest(
                2L,
                1L,
                200.0,
                25.0,
                List.of(new OrderItemRequest(10L, 2, 100.0))
        );

        OrderResponse response = orderService.createOrder(request);

        assertThat(response.id()).isEqualTo(99L);
        assertThat(response.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.finalAmount()).isEqualTo(175.0);
        assertThat(response.items()).hasSize(1);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        verify(orderSagaPublisher).publishOrderCreated(captor.getValue());

        Order saved = captor.getValue();
        assertThat(saved.getCreatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
        assertThat(saved.getItems()).hasSize(1);
        assertThat(saved.getItems().get(0).getOrder()).isSameAs(saved);
    }

    @Test
    void createOrder_shouldRejectInvalidItem() {
        OrderRequest request = new OrderRequest(
                2L,
                1L,
                100.0,
                0.0,
                List.of(new OrderItemRequest(10L, 0, 100.0))
        );

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Item quantity must be greater than zero");
    }

    @Test
    void getOrdersByCustomerId_shouldUseDeterministicRepositoryMethod() {
        Order order = new Order();
        order.setId(5L);
        order.setCustomerId(2L);
        order.setSellerId(1L);
        order.setTotalAmount(120.0);
        order.setDiscountAmount(0.0);
        order.setFinalAmount(120.0);
        order.setStatus(OrderStatus.COMPLETED);
        order.setCreatedAt(LocalDateTime.now());

        when(orderRepository.findByCustomerIdOrderByCreatedAtDescIdDesc(2L)).thenReturn(List.of(order));

        List<OrderResponse> responses = orderService.getOrdersByCustomerId(2L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).id()).isEqualTo(5L);
        verify(orderRepository).findByCustomerIdOrderByCreatedAtDescIdDesc(2L);
    }
}
