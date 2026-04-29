package com.ecommerce.payment.service;

import com.ecommerce.payment.client.OrderClient;
import com.ecommerce.payment.dto.PaymentConfirmRequest;
import com.ecommerce.payment.dto.PaymentIntentRequest;
import com.ecommerce.payment.dto.PaymentIntentResponse;
import com.ecommerce.payment.dto.PaymentResponse;
import com.ecommerce.payment.entity.Payment;
import com.ecommerce.payment.entity.PaymentStatus;
import com.ecommerce.payment.repository.PaymentRepository;
import com.iyzipay.Options;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderClient orderClient;

    @InjectMocks
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        // Force local fallback branch (no Iyzico API keys)
        Options options = new Options();
        paymentService = new PaymentService(paymentRepository, orderClient, options);
    }

    @Test
    void createPaymentIntent_shouldReturnExistingForIdempotencyKey() {
        Payment existing = new Payment();
        existing.setOrderId(56L);
        existing.setAmount(199.99);
        existing.setPaymentIntentId("intent-1");
        existing.setPaymentStatus(PaymentStatus.PENDING);
        existing.setIdempotencyKey("idem-1");

        when(paymentRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.of(existing));

        PaymentIntentResponse response = paymentService.createPaymentIntent(new PaymentIntentRequest(56L, 199.99), "idem-1");

        assertThat(response.paymentIntentId()).isEqualTo("intent-1");
        assertThat(response.orderId()).isEqualTo(56L);
        assertThat(response.status()).isEqualTo(PaymentStatus.PENDING);
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void createPaymentIntent_shouldValidatePayload() {
        assertThatThrownBy(() -> paymentService.createPaymentIntent(new PaymentIntentRequest(0L, 10.0), "x"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("orderId and amount must be valid");
    }

    @Test
    void confirmPaymentIntent_shouldMarkSuccessAndUpdateOrderStatus() {
        Payment pending = new Payment();
        pending.setId(7L);
        pending.setOrderId(56L);
        pending.setAmount(149.50);
        pending.setPaymentStatus(PaymentStatus.PENDING);
        pending.setPaymentIntentId("intent-56");
        pending.setPaymentDate(LocalDateTime.now());

        when(paymentRepository.findByPaymentIntentId("intent-56")).thenReturn(Optional.of(pending));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = paymentService.confirmPaymentIntent("intent-56", new PaymentConfirmRequest("5528790000000008"), "idem-confirm");

        assertThat(response.orderId()).isEqualTo(56L);
        assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
        verify(orderClient).updateOrderStatus(56L, "PAYMENT_AUTHORIZED");
    }

    @Test
    void confirmPaymentIntent_shouldReturnExistingSuccessWithoutProcessingAgain() {
        Payment success = new Payment();
        success.setId(11L);
        success.setOrderId(77L);
        success.setAmount(88.0);
        success.setPaymentIntentId("intent-77");
        success.setPaymentStatus(PaymentStatus.SUCCESS);
        success.setPaymentDate(LocalDateTime.now());

        when(paymentRepository.findByPaymentIntentId("intent-77")).thenReturn(Optional.of(success));

        PaymentResponse response = paymentService.confirmPaymentIntent("intent-77", new PaymentConfirmRequest("5528790000000008"), "idem-x");

        assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(orderClient, never()).updateOrderStatus(any(), any());
    }
}
