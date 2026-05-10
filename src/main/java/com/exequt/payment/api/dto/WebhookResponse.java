package com.exequt.payment.api.dto;

import com.exequt.payment.domain.PaymentWebhookProcessingResult;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WebhookResponse {

    private final PaymentWebhookProcessingResult processingResult;
    private final String message;

    public static WebhookResponse duplicate() {
        return WebhookResponse.builder()
                .processingResult(PaymentWebhookProcessingResult.DUPLICATE)
                .message("Duplicate provider event ignored")
                .build();
    }

    public static WebhookResponse processed() {
        return WebhookResponse.builder()
                .processingResult(PaymentWebhookProcessingResult.PROCESSED)
                .message("Webhook processed")
                .build();
    }

    public static WebhookResponse ignored() {
        return WebhookResponse.builder()
                .processingResult(PaymentWebhookProcessingResult.IGNORED)
                .message("Payment attempt already completed; event recorded for audit")
                .build();
    }

    public static WebhookResponse rejected(String reason) {
        return WebhookResponse.builder()
                .processingResult(PaymentWebhookProcessingResult.REJECTED)
                .message(reason)
                .build();
    }
}
