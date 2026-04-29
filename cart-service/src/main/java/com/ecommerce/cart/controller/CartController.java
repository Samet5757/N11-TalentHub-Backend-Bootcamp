package com.ecommerce.cart.controller;

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

    @GetMapping("/{cartId}")
    public ResponseEntity<CartResponse> getCart(@PathVariable Long cartId) {
        return ResponseEntity.ok(cartService.getCart(cartId));
    }

    @PostMapping
    public ResponseEntity<CartResponse> createCart(@RequestBody CartRequest request) {
        return ResponseEntity.ok(cartService.createCart(request));
    }

    @PostMapping("/{cartId}/apply-coupon")
    public ResponseEntity<CartResponse> applyCoupon(@PathVariable Long cartId, @RequestParam String code) {
        return ResponseEntity.ok(cartService.applyCoupon(cartId, code));
    }
}
