package com.ecommerce.cart.service;

import com.ecommerce.cart.client.CampaignClient;
import com.ecommerce.cart.dto.CampaignValidationResponse;
import com.ecommerce.cart.dto.CartItemRequest;
import com.ecommerce.cart.dto.CartRequest;
import com.ecommerce.cart.dto.CartResponse;
import com.ecommerce.cart.entity.Cart;
import com.ecommerce.cart.entity.CartItem;
import com.ecommerce.cart.exception.CouponValidationException;
import com.ecommerce.cart.repository.CartRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CampaignClient campaignClient;

    @InjectMocks
    private CartService cartService;

    @Test
    void addItem_shouldRecalculateTotalAndReturnFinalAmount() {
        Cart cart = new Cart();
        cart.setId(3L);
        cart.setCustomerId(2L);
        cart.setTotalAmount(0.0);
        cart.setDiscountAmount(0.0);

        when(cartRepository.findById(3L)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> {
            Cart saved = invocation.getArgument(0);
            CartItem first = saved.getItems().get(0);
            first.setId(10L);
            return saved;
        });

        CartResponse response = cartService.addItem(3L, new CartItemRequest(15L, 2, 120.0));

        assertThat(response.totalAmount()).isEqualTo(240.0);
        assertThat(response.finalAmount()).isEqualTo(240.0);
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).lineTotal()).isEqualTo(240.0);
    }

    @Test
    void applyCoupon_percentage_shouldCapByCartTotal() {
        Cart cart = new Cart();
        cart.setId(5L);
        cart.setCustomerId(2L);
        cart.setTotalAmount(100.0);
        cart.setDiscountAmount(0.0);

        when(cartRepository.findById(5L)).thenReturn(Optional.of(cart));
        when(campaignClient.validateCampaign("BIGSALE")).thenReturn(new CampaignValidationResponse(
                1L,
                "BIGSALE",
                "PERCENTAGE",
                150.0,
                true
        ));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartResponse response = cartService.applyCoupon(5L, "BIGSALE");

        assertThat(response.discountAmount()).isEqualTo(100.0);
        assertThat(response.finalAmount()).isEqualTo(0.0);
        assertThat(response.couponCode()).isEqualTo("BIGSALE");
    }

    @Test
    void applyCoupon_shouldRejectUnsupportedDiscountType() {
        Cart cart = new Cart();
        cart.setId(6L);
        cart.setCustomerId(2L);
        cart.setTotalAmount(100.0);
        cart.setDiscountAmount(0.0);

        when(cartRepository.findById(6L)).thenReturn(Optional.of(cart));
        when(campaignClient.validateCampaign("X")).thenReturn(new CampaignValidationResponse(
                2L,
                "X",
                "UNKNOWN",
                10.0,
                true
        ));

        assertThatThrownBy(() -> cartService.applyCoupon(6L, "X"))
                .isInstanceOf(CouponValidationException.class)
                .hasMessageContaining("Unsupported discount type");
    }

    @Test
    void createCart_shouldRejectInvalidInput() {
        assertThatThrownBy(() -> cartService.createCart(new CartRequest(null, 10.0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cart customerId and totalAmount are required");
    }

    @Test
    void removeCoupon_shouldResetDiscountFields() {
        Cart cart = new Cart();
        cart.setId(7L);
        cart.setCustomerId(2L);
        cart.setTotalAmount(250.0);
        cart.setDiscountAmount(40.0);
        cart.setCouponCode("SAVE40");

        when(cartRepository.findById(7L)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartResponse response = cartService.removeCoupon(7L);

        assertThat(response.discountAmount()).isEqualTo(0.0);
        assertThat(response.finalAmount()).isEqualTo(250.0);
        assertThat(response.couponCode()).isNull();
        verify(cartRepository).save(any(Cart.class));
    }
}
