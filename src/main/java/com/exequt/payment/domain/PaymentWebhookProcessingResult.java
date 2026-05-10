package com.exequt.payment.domain;

public enum PaymentWebhookProcessingResult {
    PROCESSED,
    DUPLICATE,
    IGNORED,
    REJECTED
}
