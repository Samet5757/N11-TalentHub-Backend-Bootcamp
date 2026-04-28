package com.ecommerce.cart.controller;

import com.ecommerce.cart.entity.CartItem;
import com.ecommerce.cart.service.CartService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cart")
public class CartController {
    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public List<CartItem> getCart() {
        return cartService.getAllItems();
    }

    @PostMapping("/add")
    public ResponseEntity<CartItem> addItem(@RequestBody CartItem item) {
        CartItem added = cartService.addItem(item);
        return ResponseEntity.ok(added);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeItem(@PathVariable Long id) {
        cartService.removeItem(id);
        return ResponseEntity.noContent().build();
    }
}
