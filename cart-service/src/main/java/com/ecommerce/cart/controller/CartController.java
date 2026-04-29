package com.ecommerce.cart.controller;

import com.ecommerce.cart.dto.CartItemRequest;
import com.ecommerce.cart.dto.CartRequest;
import com.ecommerce.cart.dto.CartResponse;
import com.ecommerce.cart.service.CartService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/carts")
public class CartController {
    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public List<CartResponse> getCarts() {
        return cartService.getAllCarts();
    }

    @GetMapping("/customer/{customerId}")
    public List<CartResponse> getCartsByCustomerId(@PathVariable Long customerId) {
        return cartService.getCartsByCustomerId(customerId);
    }

    @GetMapping("/{cartId}")
    public ResponseEntity<CartResponse> getCart(@PathVariable Long cartId) {
        return ResponseEntity.ok(cartService.getCart(cartId));
    }

    @PostMapping
    public ResponseEntity<CartResponse> createCart(@RequestBody CartRequest request) {
        return ResponseEntity.ok(cartService.createCart(request));
    }

    @PostMapping("/{cartId}/items")
    public ResponseEntity<CartResponse> addItem(@PathVariable Long cartId, @RequestBody CartItemRequest request) {
        return ResponseEntity.ok(cartService.addItem(cartId, request));
    }

    @PutMapping("/{cartId}/items/{itemId}")
    public ResponseEntity<CartResponse> updateItem(@PathVariable Long cartId, @PathVariable Long itemId, @RequestBody CartItemRequest request) {
        return ResponseEntity.ok(cartService.updateItem(cartId, itemId, request));
    }

    @DeleteMapping("/{cartId}/items/{itemId}")
    public ResponseEntity<CartResponse> removeItem(@PathVariable Long cartId, @PathVariable Long itemId) {
        return ResponseEntity.ok(cartService.removeItem(cartId, itemId));
    }

    @PostMapping("/{cartId}/apply-coupon")
    public ResponseEntity<CartResponse> applyCoupon(@PathVariable Long cartId, @RequestParam String code) {
        return ResponseEntity.ok(cartService.applyCoupon(cartId, code));
    }

    @DeleteMapping("/{cartId}/coupon")
    public ResponseEntity<CartResponse> removeCoupon(@PathVariable Long cartId) {
        return ResponseEntity.ok(cartService.removeCoupon(cartId));
    }

    @DeleteMapping("/{cartId}")
    public ResponseEntity<Void> deleteCart(@PathVariable Long cartId) {
        cartService.deleteCart(cartId);
        return ResponseEntity.noContent().build();
    }
}
