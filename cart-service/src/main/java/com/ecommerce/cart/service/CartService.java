package com.ecommerce.cart.service;

import com.ecommerce.cart.entity.CartItem;
import com.ecommerce.cart.repository.CartRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CartService {
    private final CartRepository cartRepository;

    public CartService(CartRepository cartRepository) {
        this.cartRepository = cartRepository;
    }

    public List<CartItem> getAllItems() {
        return cartRepository.findAll();
    }

    public CartItem addItem(CartItem item) {
        return cartRepository.save(item);
    }

    public void removeItem(Long itemId) {
        cartRepository.deleteById(itemId);
    }
}
