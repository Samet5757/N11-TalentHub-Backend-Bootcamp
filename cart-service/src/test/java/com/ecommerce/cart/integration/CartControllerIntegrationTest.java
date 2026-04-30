package com.ecommerce.cart.integration;

import com.ecommerce.cart.client.CampaignClient;
import com.ecommerce.cart.dto.CampaignValidationResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CartControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CampaignClient campaignClient;

    @Test
    void shouldCreateCartAddItemAndApplyCoupon() throws Exception {
        String createBody = objectMapper.writeValueAsString(new CreateCartPayload(2L, 0.0));

        String createResponse = mockMvc.perform(post("/carts")
                        .header("X-User-Id", "2")
                        .header("X-User-Role", "CUSTOMER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(2))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long cartId = objectMapper.readTree(createResponse).get("id").asLong();

        String addItemBody = objectMapper.writeValueAsString(new CartItemPayload(101L, 2, 150.0));
        mockMvc.perform(post("/carts/" + cartId + "/items")
                        .header("X-User-Id", "2")
                        .header("X-User-Role", "CUSTOMER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addItemBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(300.0))
                .andExpect(jsonPath("$.finalAmount").value(300.0));

        when(campaignClient.validateCampaign("SAVE10"))
                .thenReturn(new CampaignValidationResponse(1L, "SAVE10", "PERCENTAGE", 10.0, true));

        mockMvc.perform(post("/carts/" + cartId + "/apply-coupon?code=SAVE10")
                        .header("X-User-Id", "2")
                        .header("X-User-Role", "CUSTOMER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.couponCode").value("SAVE10"))
                .andExpect(jsonPath("$.discountAmount").value(30.0))
                .andExpect(jsonPath("$.finalAmount").value(270.0));
    }

    private record CreateCartPayload(Long customerId, Double totalAmount) {}

    private record CartItemPayload(Long productId, Integer quantity, Double unitPrice) {}
}
