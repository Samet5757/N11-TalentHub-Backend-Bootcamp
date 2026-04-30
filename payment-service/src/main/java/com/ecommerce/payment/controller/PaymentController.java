package com.ecommerce.payment.controller;

import com.ecommerce.payment.dto.*;
import com.ecommerce.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/payments")
public class PaymentController {
    private static final Set<String> PRIVILEGED_ROLES = Set.of("ADMIN", "SELLER");
    private static final Set<String> USER_ROLES = Set.of("CUSTOMER", "ADMIN", "SELLER");

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping
    public List<PaymentResponse> getAllPayments(@RequestHeader HttpHeaders headers) {
        requireRole(headers, PRIVILEGED_ROLES);
        return paymentService.getAllPayments();
    }

    @PostMapping("/intents")
    public ResponseEntity<PaymentIntentResponse> createIntent(
            @Valid @RequestBody PaymentIntentRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader HttpHeaders headers) {
        requireRole(headers, USER_ROLES);
        return ResponseEntity.ok(paymentService.createPaymentIntent(request, idempotencyKey));
    }

    @PostMapping("/intents/{paymentIntentId}/confirm")
    public ResponseEntity<PaymentResponse> confirmIntent(
            @PathVariable String paymentIntentId,
            @Valid @RequestBody PaymentConfirmRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader HttpHeaders headers) {
        requireRole(headers, USER_ROLES);
        return ResponseEntity.ok(paymentService.confirmPaymentIntent(paymentIntentId, request, idempotencyKey));
    }

    @PostMapping("/pay")
    public ResponseEntity<PaymentResponse> pay(
            @Valid @RequestBody PaymentRequest paymentRequest,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader HttpHeaders headers) {
        requireRole(headers, USER_ROLES);
        PaymentResponse processed = paymentService.processPayment(paymentRequest, idempotencyKey);
        return ResponseEntity.ok(processed);
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<PaymentResponse> getPaymentByOrderId(@PathVariable Long orderId, @RequestHeader HttpHeaders headers) {
        requireRole(headers, USER_ROLES);
        PaymentResponse payment = paymentService.getPaymentByOrderId(orderId);
        return ResponseEntity.ok(payment);
    }

    private void requireRole(HttpHeaders headers, Set<String> allowedRoles) {
        String role = headers.getFirst("X-User-Role");
        if (role == null || role.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "missing X-User-Role header");
        }
        String normalizedRole = role.trim().toUpperCase();
        if (!allowedRoles.contains(normalizedRole)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "insufficient role");
        }
    }
}
