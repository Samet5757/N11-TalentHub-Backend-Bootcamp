package com.ecommerce.payment.controller;

import com.ecommerce.payment.dto.*;
import com.ecommerce.payment.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping
    public List<PaymentResponse> getAllPayments() {
        return paymentService.getAllPayments();
    }

    @PostMapping("/intents")
    public ResponseEntity<PaymentIntentResponse> createIntent(
            @RequestBody PaymentIntentRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return ResponseEntity.ok(paymentService.createPaymentIntent(request, idempotencyKey));
    }

    @PostMapping("/intents/{paymentIntentId}/confirm")
    public ResponseEntity<PaymentResponse> confirmIntent(
            @PathVariable String paymentIntentId,
            @RequestBody PaymentConfirmRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return ResponseEntity.ok(paymentService.confirmPaymentIntent(paymentIntentId, request, idempotencyKey));
    }

    @PostMapping("/pay")
    public ResponseEntity<PaymentResponse> pay(
            @RequestBody PaymentRequest paymentRequest,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        PaymentResponse processed = paymentService.processPayment(paymentRequest, idempotencyKey);
        return ResponseEntity.ok(processed);
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<PaymentResponse> getPaymentByOrderId(@PathVariable Long orderId) {
        PaymentResponse payment = paymentService.getPaymentByOrderId(orderId);
        return ResponseEntity.ok(payment);
    }
}
