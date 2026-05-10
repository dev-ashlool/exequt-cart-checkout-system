package com.exequt.payment.api.dto;

import com.exequt.payment.domain.PaymentAttemptStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PaymentAttemptResponse {

    private final Long id;
    private final Long orderId;
    private final BigDecimal amount;
    private final String providerReference;
    private final PaymentAttemptStatus status;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
}
