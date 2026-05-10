package com.exequt.payment.application;

import com.exequt.payment.api.dto.PaymentAttemptResponse;
import com.exequt.payment.domain.PaymentAttempt;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public PaymentAttemptResponse toPaymentAttemptResponse(PaymentAttempt attempt) {
        return PaymentAttemptResponse.builder()
                .id(attempt.getId())
                .orderId(attempt.getOrderId())
                .amount(attempt.getAmount())
                .providerReference(attempt.getProviderReference())
                .status(attempt.getStatus())
                .createdAt(attempt.getCreatedAt())
                .updatedAt(attempt.getUpdatedAt())
                .build();
    }
}
