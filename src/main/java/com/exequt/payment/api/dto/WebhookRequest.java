package com.exequt.payment.api.dto;

import com.exequt.payment.domain.WebhookProviderStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class WebhookRequest {

    @NotBlank(message = "providerEventId is required")
    private String providerEventId;

    @NotNull(message = "paymentAttemptId is required")
    private Long paymentAttemptId;

    @NotNull(message = "status is required")
    private WebhookProviderStatus status;

    @NotNull(message = "amount is required")
    @Positive(message = "amount must be positive")
    private BigDecimal amount;
}
