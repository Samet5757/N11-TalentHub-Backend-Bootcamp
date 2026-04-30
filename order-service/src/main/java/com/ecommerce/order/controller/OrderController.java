package com.ecommerce.order.controller;

import com.ecommerce.order.dto.OrderRequest;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/orders")
public class OrderController {
    private static final Set<String> PRIVILEGED_ROLES = Set.of("ADMIN", "SELLER");
    private static final String CUSTOMER_ROLE = "CUSTOMER";

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public List<OrderResponse> getAllOrders(@RequestHeader HttpHeaders headers) {
        requireRole(headers, PRIVILEGED_ROLES);
        return orderService.getAllOrders();
    }

    @GetMapping("/customer/{customerId}")
    public List<OrderResponse> getOrdersByCustomerId(@PathVariable Long customerId, @RequestHeader HttpHeaders headers) {
        authorizeCustomerResource(headers, customerId);
        return orderService.getOrdersByCustomerId(customerId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable Long id, @RequestHeader HttpHeaders headers) {
        OrderResponse order = orderService.getOrderById(id);
        authorizeCustomerResource(headers, order.customerId());
        return ResponseEntity.ok(order);
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody OrderRequest order, @RequestHeader HttpHeaders headers) {
        Long userId = requireCustomer(headers);
        if (order == null || order.customerId() == null || !order.customerId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "customerId does not match token user");
        }
        OrderResponse created = orderService.createOrder(order);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<OrderResponse> updateOrderStatus(@PathVariable Long id, @RequestParam String status, @RequestHeader HttpHeaders headers) {
        if (!isInternalPaymentCall(headers)) {
            requireRole(headers, PRIVILEGED_ROLES);
        }
        OrderResponse updated = orderService.updateOrderStatus(id, status);
        return ResponseEntity.ok(updated);
    }

    private boolean isInternalPaymentCall(HttpHeaders headers) {
        String source = headers.getFirst("X-Internal-Service");
        return "payment-service".equalsIgnoreCase(source);
    }

    private void authorizeCustomerResource(HttpHeaders headers, Long resourceCustomerId) {
        String role = extractRole(headers);
        if (PRIVILEGED_ROLES.contains(role)) {
            return;
        }
        if (!CUSTOMER_ROLE.equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "insufficient role");
        }
        Long userId = extractUserId(headers);
        if (!userId.equals(resourceCustomerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "resource does not belong to user");
        }
    }

    private Long requireCustomer(HttpHeaders headers) {
        String role = extractRole(headers);
        if (!CUSTOMER_ROLE.equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "customer role required");
        }
        return extractUserId(headers);
    }

    private void requireRole(HttpHeaders headers, Set<String> allowedRoles) {
        String role = extractRole(headers);
        if (!allowedRoles.contains(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "insufficient role");
        }
    }

    private String extractRole(HttpHeaders headers) {
        String role = headers.getFirst("X-User-Role");
        if (role == null || role.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "missing X-User-Role header");
        }
        return role.trim().toUpperCase();
    }

    private Long extractUserId(HttpHeaders headers) {
        String userId = headers.getFirst("X-User-Id");
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "missing X-User-Id header");
        }
        try {
            return Long.valueOf(userId.trim());
        } catch (NumberFormatException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid X-User-Id header");
        }
    }
}
