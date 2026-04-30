package com.ecommerce.payment.integration;

import com.ecommerce.payment.client.OrderClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaymentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderClient orderClient;

    @Test
    void shouldCreateAndConfirmPaymentIntent() throws Exception {
        String createIntentBody = objectMapper.writeValueAsString(new IntentPayload(55L, 300.0));

        String createResponse = mockMvc.perform(post("/payments/intents")
                        .header("Idempotency-Key", "intent-55")
                        .header("X-User-Id", "2")
                        .header("X-User-Role", "CUSTOMER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createIntentBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(55))
                .andExpect(jsonPath("$.amount").value(300.0))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String paymentIntentId = objectMapper.readTree(createResponse).get("paymentIntentId").asText();
        String confirmBody = objectMapper.writeValueAsString(new ConfirmPayload("5528790000000008"));

        mockMvc.perform(post("/payments/intents/" + paymentIntentId + "/confirm")
                        .header("Idempotency-Key", "confirm-55")
                        .header("X-User-Id", "2")
                        .header("X-User-Role", "CUSTOMER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(55))
                .andExpect(jsonPath("$.paymentStatus").value("SUCCESS"));

        verify(orderClient).updateOrderStatus(55L, "PAYMENT_AUTHORIZED");
    }

    private record IntentPayload(Long orderId, Double amount) {}

    private record ConfirmPayload(String cardNumber) {}
}
