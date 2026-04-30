package com.ecommerce.order.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreateOrderAndListByCustomer() throws Exception {
        String requestBody = objectMapper.writeValueAsString(
                new OrderPayload(
                        2L,
                        1L,
                        500.0,
                        50.0,
                        List.of(new OrderItemPayload(11L, 2, 250.0))
                )
        );

        mockMvc.perform(post("/orders")
                        .header("X-User-Id", "2")
                        .header("X-User-Role", "CUSTOMER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(2))
                .andExpect(jsonPath("$.sellerId").value(1))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.finalAmount").value(450.0))
                .andExpect(jsonPath("$.items[0].productId").value(11));

        mockMvc.perform(get("/orders/customer/2")
                        .header("X-User-Id", "2")
                        .header("X-User-Role", "CUSTOMER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].customerId").value(2))
                .andExpect(jsonPath("$[0].items[0].quantity").value(2));
    }

    private record OrderPayload(
            Long customerId,
            Long sellerId,
            Double totalAmount,
            Double discountAmount,
            List<OrderItemPayload> items
    ) {}

    private record OrderItemPayload(Long productId, Integer quantity, Double unitPrice) {}
}
