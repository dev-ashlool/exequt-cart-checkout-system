package com.exequt.payment.application;

import com.exequt.payment.api.dto.PaymentAttemptResponse;
import lombok.Getter;

@Getter
public class PaymentStartResult {

    private final PaymentAttemptResponse attempt;
    private final boolean created;

    public PaymentStartResult(PaymentAttemptResponse attempt, boolean created) {
        this.attempt = attempt;
        this.created = created;
    }
}
