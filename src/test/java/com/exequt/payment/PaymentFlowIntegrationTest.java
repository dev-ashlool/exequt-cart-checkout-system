package com.exequt.payment;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.exequt.order.domain.OrderStatus;
import com.exequt.payment.domain.PaymentAttemptStatus;
import com.exequt.payment.domain.PaymentWebhookProcessingResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaymentFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void successfulPayment_confirmsOrder() throws Exception {
        long orderId = checkoutNewOrder("SKU-A", new BigDecimal("25.00"));
        MvcResult start = mockMvc.perform(post("/orders/{orderId}/payment/start", orderId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status", is(PaymentAttemptStatus.INITIATED.name())))
                .andReturn();
        long attemptId = readData(start).path("id").asLong();
        BigDecimal amount = new BigDecimal(readData(start).path("amount").asText());

        String eventId = "evt-" + UUID.randomUUID();
        mockMvc.perform(post("/payments/mock-provider/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(webhookJson(eventId, attemptId, "CONFIRMED", amount)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.processingResult", is(PaymentWebhookProcessingResult.PROCESSED.name())));

        mockMvc.perform(get("/orders/{orderId}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is(OrderStatus.PAID.name())));
    }

    @Test
    void failedPayment_retryThenSuccess() throws Exception {
        long orderId = checkoutNewOrder("SKU-B", new BigDecimal("40.00"));
        MvcResult firstStart = mockMvc.perform(post("/orders/{orderId}/payment/start", orderId))
                .andExpect(status().isCreated())
                .andReturn();
        long attempt1 = readData(firstStart).path("id").asLong();
        BigDecimal amount = new BigDecimal(readData(firstStart).path("amount").asText());

        String failEvent = "evt-fail-" + UUID.randomUUID();
        mockMvc.perform(post("/payments/mock-provider/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(webhookJson(failEvent, attempt1, "FAILED", amount)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.processingResult", is(PaymentWebhookProcessingResult.PROCESSED.name())));

        mockMvc.perform(get("/orders/{orderId}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is(OrderStatus.PAYMENT_FAILED.name())));

        MvcResult secondStart = mockMvc.perform(post("/orders/{orderId}/payment/start", orderId))
                .andExpect(status().isCreated())
                .andReturn();
        long attempt2 = readData(secondStart).path("id").asLong();
        assertNotEquals(attempt1, attempt2);

        String okEvent = "evt-ok-" + UUID.randomUUID();
        mockMvc.perform(post("/payments/mock-provider/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(webhookJson(okEvent, attempt2, "CONFIRMED", amount)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/orders/{orderId}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is(OrderStatus.PAID.name())));
    }

    @Test
    void duplicateWebhook_isIdempotent() throws Exception {
        long orderId = checkoutNewOrder("SKU-C", new BigDecimal("15.00"));
        MvcResult start = mockMvc.perform(post("/orders/{orderId}/payment/start", orderId))
                .andExpect(status().isCreated())
                .andReturn();
        long attemptId = readData(start).path("id").asLong();
        BigDecimal amount = new BigDecimal(readData(start).path("amount").asText());
        String eventId = "evt-dup-" + UUID.randomUUID();

        mockMvc.perform(post("/payments/mock-provider/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(webhookJson(eventId, attemptId, "CONFIRMED", amount)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.processingResult", is(PaymentWebhookProcessingResult.PROCESSED.name())));

        mockMvc.perform(post("/payments/mock-provider/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(webhookJson(eventId, attemptId, "CONFIRMED", amount)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.processingResult", is(PaymentWebhookProcessingResult.DUPLICATE.name())));
    }

    @Test
    void duplicatePaymentStart_returnsExistingInitiatedAttempt() throws Exception {
        long orderId = checkoutNewOrder("SKU-D", new BigDecimal("12.00"));
        MvcResult first = mockMvc.perform(post("/orders/{orderId}/payment/start", orderId))
                .andExpect(status().isCreated())
                .andReturn();
        long attemptId = readData(first).path("id").asLong();

        MvcResult second = mockMvc.perform(post("/orders/{orderId}/payment/start", orderId))
                .andExpect(status().isOk())
                .andReturn();
        long secondId = readData(second).path("id").asLong();
        assertEquals(attemptId, secondId);
    }

    @Test
    void conflictingWebhook_afterTerminal_isIgnored() throws Exception {
        long orderId = checkoutNewOrder("SKU-E", new BigDecimal("18.00"));
        MvcResult start = mockMvc.perform(post("/orders/{orderId}/payment/start", orderId))
                .andExpect(status().isCreated())
                .andReturn();
        long attemptId = readData(start).path("id").asLong();
        BigDecimal amount = new BigDecimal(readData(start).path("amount").asText());

        String confirmedEvt = "evt-conf-" + UUID.randomUUID();
        mockMvc.perform(post("/payments/mock-provider/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(webhookJson(confirmedEvt, attemptId, "CONFIRMED", amount)))
                .andExpect(status().isOk());

        String conflictEvt = "evt-conflict-" + UUID.randomUUID();
        mockMvc.perform(post("/payments/mock-provider/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(webhookJson(conflictEvt, attemptId, "FAILED", amount)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.processingResult", is(PaymentWebhookProcessingResult.IGNORED.name())));

        mockMvc.perform(get("/orders/{orderId}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is(OrderStatus.PAID.name())));
    }

    private long checkoutNewOrder(String productId, BigDecimal price) throws Exception {
        MvcResult cartResult =
                mockMvc.perform(post("/carts")).andExpect(status().isCreated()).andReturn();
        long cartId = readData(cartResult).path("id").asLong();

        String addItem = String.format(
                "{\"productId\":\"%s\",\"quantity\":1,\"price\":%s}", productId, price.toPlainString());
        mockMvc.perform(post("/carts/{cartId}/items", cartId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addItem))
                .andExpect(status().isOk());

        MvcResult checkoutResult = mockMvc.perform(post("/carts/{cartId}/checkout", cartId))
                .andExpect(status().isCreated())
                .andReturn();
        return readData(checkoutResult).path("id").asLong();
    }

    private JsonNode readData(MvcResult result) throws Exception {
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("data");
    }

    private String webhookJson(String providerEventId, long paymentAttemptId, String status, BigDecimal amount) {
        return String.format(
                "{\"providerEventId\":\"%s\",\"paymentAttemptId\":%d,\"status\":\"%s\",\"amount\":%s}",
                providerEventId, paymentAttemptId, status, amount.toPlainString());
    }
}
