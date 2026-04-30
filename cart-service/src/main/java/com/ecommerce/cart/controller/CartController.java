package com.ecommerce.cart.controller;

import com.ecommerce.cart.dto.CartItemRequest;
import com.ecommerce.cart.dto.CartRequest;
import com.ecommerce.cart.dto.CartResponse;
import com.ecommerce.cart.service.CartService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/carts")
public class CartController {
    private static final Set<String> PRIVILEGED_ROLES = Set.of("ADMIN", "SELLER");
    private static final String CUSTOMER_ROLE = "CUSTOMER";
    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public List<CartResponse> getCarts(@RequestHeader HttpHeaders headers) {
        requireRole(headers, PRIVILEGED_ROLES);
        return cartService.getAllCarts();
    }

    @GetMapping("/customer/{customerId}")
    public List<CartResponse> getCartsByCustomerId(@PathVariable Long customerId, @RequestHeader HttpHeaders headers) {
        authorizeCustomerResource(headers, customerId);
        return cartService.getCartsByCustomerId(customerId);
    }

    @GetMapping("/{cartId}")
    public ResponseEntity<CartResponse> getCart(@PathVariable Long cartId, @RequestHeader HttpHeaders headers) {
        CartResponse cart = cartService.getCart(cartId);
        authorizeCustomerResource(headers, cart.customerId());
        return ResponseEntity.ok(cart);
    }

    @PostMapping
    public ResponseEntity<CartResponse> createCart(@Valid @RequestBody CartRequest request, @RequestHeader HttpHeaders headers) {
        Long userId = requireCustomer(headers);
        if (request == null || request.customerId() == null || !request.customerId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "customerId does not match token user");
        }
        return ResponseEntity.ok(cartService.createCart(request));
    }

    @PostMapping("/{cartId}/items")
    public ResponseEntity<CartResponse> addItem(@PathVariable Long cartId, @Valid @RequestBody CartItemRequest request, @RequestHeader HttpHeaders headers) {
        CartResponse cart = cartService.getCart(cartId);
        authorizeCustomerResource(headers, cart.customerId());
        return ResponseEntity.ok(cartService.addItem(cartId, request));
    }

    @PutMapping("/{cartId}/items/{itemId}")
    public ResponseEntity<CartResponse> updateItem(@PathVariable Long cartId, @PathVariable Long itemId, @Valid @RequestBody CartItemRequest request, @RequestHeader HttpHeaders headers) {
        CartResponse cart = cartService.getCart(cartId);
        authorizeCustomerResource(headers, cart.customerId());
        return ResponseEntity.ok(cartService.updateItem(cartId, itemId, request));
    }

    @DeleteMapping("/{cartId}/items/{itemId}")
    public ResponseEntity<CartResponse> removeItem(@PathVariable Long cartId, @PathVariable Long itemId, @RequestHeader HttpHeaders headers) {
        CartResponse cart = cartService.getCart(cartId);
        authorizeCustomerResource(headers, cart.customerId());
        return ResponseEntity.ok(cartService.removeItem(cartId, itemId));
    }

    @PostMapping("/{cartId}/apply-coupon")
    public ResponseEntity<CartResponse> applyCoupon(@PathVariable Long cartId, @RequestParam String code, @RequestHeader HttpHeaders headers) {
        CartResponse cart = cartService.getCart(cartId);
        authorizeCustomerResource(headers, cart.customerId());
        return ResponseEntity.ok(cartService.applyCoupon(cartId, code));
    }

    @DeleteMapping("/{cartId}/coupon")
    public ResponseEntity<CartResponse> removeCoupon(@PathVariable Long cartId, @RequestHeader HttpHeaders headers) {
        CartResponse cart = cartService.getCart(cartId);
        authorizeCustomerResource(headers, cart.customerId());
        return ResponseEntity.ok(cartService.removeCoupon(cartId));
    }

    @DeleteMapping("/{cartId}")
    public ResponseEntity<Void> deleteCart(@PathVariable Long cartId, @RequestHeader HttpHeaders headers) {
        CartResponse cart = cartService.getCart(cartId);
        authorizeCustomerResource(headers, cart.customerId());
        cartService.deleteCart(cartId);
        return ResponseEntity.noContent().build();
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
