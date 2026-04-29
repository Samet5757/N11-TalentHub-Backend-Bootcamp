package com.ecommerce.cart.service;

import com.ecommerce.cart.client.CampaignClient;
import com.ecommerce.cart.dto.CampaignValidationResponse;
import com.ecommerce.cart.dto.CartRequest;
import com.ecommerce.cart.dto.CartResponse;
import com.ecommerce.cart.entity.Cart;
import com.ecommerce.cart.exception.CartNotFoundException;
import com.ecommerce.cart.exception.CouponValidationException;
import com.ecommerce.cart.repository.CartRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CartService {
    private final CartRepository cartRepository;
    private final CampaignClient campaignClient;

    public CartService(CartRepository cartRepository, CampaignClient campaignClient) {
        this.cartRepository = cartRepository;
        this.campaignClient = campaignClient;
    }

    public List<CartResponse> getAllCarts() {
        return cartRepository.findAll().stream().map(this::toResponse).toList();
    }

    public CartResponse getCart(Long cartId) {
        return toResponse(findCart(cartId));
    }

    public CartResponse createCart(CartRequest request) {
        if (request == null || request.totalAmount() == null || request.totalAmount() < 0) {
            throw new IllegalArgumentException("Cart totalAmount must be zero or greater");
        }
        Cart cart = new Cart();
        cart.setTotalAmount(request.totalAmount());
        cart.setDiscountAmount(0.0);
        return toResponse(cartRepository.save(cart));
    }

    public CartResponse applyCoupon(Long cartId, String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Coupon code is required");
        }

        Cart cart = findCart(cartId);
        CampaignValidationResponse campaign = campaignClient.validateCampaign(code.trim());

        double discountAmount = calculateDiscount(cart.getTotalAmount(), campaign);
        cart.setCouponCode(campaign.code());
        cart.setDiscountAmount(discountAmount);

        return toResponse(cartRepository.save(cart));
    }

    private Cart findCart(Long cartId) {
        return cartRepository.findById(cartId).orElseThrow(() -> new CartNotFoundException(cartId));
    }

    private double calculateDiscount(Double totalAmount, CampaignValidationResponse campaign) {
        if (campaign.discountType() == null || campaign.discountValue() == null) {
            throw new CouponValidationException("Campaign data is incomplete");
        }

        double amount = totalAmount == null ? 0.0 : totalAmount;
        double rawDiscount;
        if ("PERCENTAGE".equalsIgnoreCase(campaign.discountType())) {
            rawDiscount = amount * campaign.discountValue() / 100.0;
        } else if ("FLAT_AMOUNT".equalsIgnoreCase(campaign.discountType())) {
            rawDiscount = campaign.discountValue();
        } else {
            throw new CouponValidationException("Unsupported discount type: " + campaign.discountType());
        }

        if (rawDiscount < 0) {
            throw new CouponValidationException("Calculated discount cannot be negative");
        }
        return Math.min(rawDiscount, amount);
    }

    private CartResponse toResponse(Cart cart) {
        double total = cart.getTotalAmount() == null ? 0.0 : cart.getTotalAmount();
        double discount = cart.getDiscountAmount() == null ? 0.0 : cart.getDiscountAmount();
        return new CartResponse(
                cart.getId(),
                total,
                cart.getCouponCode(),
                discount,
                Math.max(0.0, total - discount)
        );
    }
}
