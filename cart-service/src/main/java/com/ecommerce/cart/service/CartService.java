package com.ecommerce.cart.service;

import com.ecommerce.cart.client.CampaignClient;
import com.ecommerce.cart.dto.*;
import com.ecommerce.cart.entity.Cart;
import com.ecommerce.cart.entity.CartItem;
import com.ecommerce.cart.exception.CartNotFoundException;
import com.ecommerce.cart.exception.CouponValidationException;
import com.ecommerce.cart.repository.CartRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CartService {
    private static final Logger log = LoggerFactory.getLogger(CartService.class);
    private final CartRepository cartRepository;
    private final CampaignClient campaignClient;

    public CartService(CartRepository cartRepository, CampaignClient campaignClient) {
        this.cartRepository = cartRepository;
        this.campaignClient = campaignClient;
    }

    public List<CartResponse> getAllCarts() {
        return cartRepository.findAll().stream().map(this::toResponse).toList();
    }

    public List<CartResponse> getCartsByCustomerId(Long customerId) {
        return cartRepository.findByCustomerId(customerId).stream().map(this::toResponse).toList();
    }

    public CartResponse getCart(Long cartId) {
        return toResponse(findCart(cartId));
    }

    public CartResponse createCart(CartRequest request) {
        if (request == null || request.totalAmount() == null || request.totalAmount() < 0 || request.customerId() == null) {
            throw new IllegalArgumentException("Cart customerId and totalAmount are required");
        }
        Cart cart = new Cart();
        cart.setCustomerId(request.customerId());
        cart.setTotalAmount(request.totalAmount());
        cart.setDiscountAmount(0.0);
        log.info("Creating cart for customerId={}", request.customerId());
        return toResponse(cartRepository.save(cart));
    }

    public CartResponse addItem(Long cartId, CartItemRequest request) {
        validateItemRequest(request);
        Cart cart = findCart(cartId);
        log.info("Adding item to cartId={}, productId={}, quantity={}", cartId, request.productId(), request.quantity());

        CartItem item = new CartItem();
        item.setCart(cart);
        item.setProductId(request.productId());
        item.setQuantity(request.quantity());
        item.setUnitPrice(request.unitPrice());
        cart.getItems().add(item);

        recalculateTotal(cart);
        return toResponse(cartRepository.save(cart));
    }

    public CartResponse updateItem(Long cartId, Long itemId, CartItemRequest request) {
        validateItemRequest(request);
        Cart cart = findCart(cartId);

        CartItem item = cart.getItems().stream()
                .filter(it -> it.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Cart item not found: " + itemId));

        item.setProductId(request.productId());
        item.setQuantity(request.quantity());
        item.setUnitPrice(request.unitPrice());
        recalculateTotal(cart);
        return toResponse(cartRepository.save(cart));
    }

    public CartResponse removeItem(Long cartId, Long itemId) {
        Cart cart = findCart(cartId);
        cart.getItems().removeIf(item -> item.getId().equals(itemId));
        recalculateTotal(cart);
        return toResponse(cartRepository.save(cart));
    }

    public void deleteCart(Long cartId) {
        cartRepository.delete(findCart(cartId));
    }

    public CartResponse applyCoupon(Long cartId, String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Coupon code is required");
        }

        Cart cart = findCart(cartId);
        CampaignValidationResponse campaign = campaignClient.validateCampaign(code.trim());
        log.info("Applying coupon to cartId={}, code={}", cartId, code);

        double discountAmount = calculateDiscount(cart.getTotalAmount(), campaign);
        cart.setCouponCode(campaign.code());
        cart.setDiscountAmount(discountAmount);

        return toResponse(cartRepository.save(cart));
    }

    public CartResponse removeCoupon(Long cartId) {
        Cart cart = findCart(cartId);
        log.info("Removing coupon from cartId={}", cartId);
        cart.setCouponCode(null);
        cart.setDiscountAmount(0.0);
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

    private void validateItemRequest(CartItemRequest request) {
        if (request == null || request.productId() == null || request.productId() <= 0 || request.quantity() == null || request.quantity() <= 0 || request.unitPrice() == null || request.unitPrice() <= 0) {
            throw new IllegalArgumentException("productId, quantity and unitPrice must be valid");
        }
    }

    private void recalculateTotal(Cart cart) {
        double total = cart.getItems().stream()
                .mapToDouble(item -> item.getQuantity() * item.getUnitPrice())
                .sum();
        cart.setTotalAmount(total);
        if (cart.getDiscountAmount() != null && cart.getDiscountAmount() > total) {
            cart.setDiscountAmount(total);
        }
    }

    private CartResponse toResponse(Cart cart) {
        double total = cart.getTotalAmount() == null ? 0.0 : cart.getTotalAmount();
        double discount = cart.getDiscountAmount() == null ? 0.0 : cart.getDiscountAmount();
        List<CartItemResponse> items = cart.getItems().stream()
                .map(item -> new CartItemResponse(item.getId(), item.getProductId(), item.getQuantity(), item.getUnitPrice(), item.getQuantity() * item.getUnitPrice()))
                .toList();
        return new CartResponse(
                cart.getId(),
                cart.getCustomerId(),
                total,
                cart.getCouponCode(),
                discount,
                Math.max(0.0, total - discount),
                items
        );
    }
}
