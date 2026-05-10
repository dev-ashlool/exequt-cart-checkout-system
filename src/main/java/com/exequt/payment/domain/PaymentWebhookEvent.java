package com.exequt.payment.domain;

import com.exequt.common.exception.BusinessException;
import com.exequt.common.response.ResultCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "payment_webhook_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentWebhookEvent {

    private static final int MONEY_SCALE = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider_event_id", nullable = false, unique = true, length = 255)
    private String providerEventId;

    @Column(name = "payment_attempt_id", nullable = false)
    private Long paymentAttemptId;

    @Column(name = "webhook_status", nullable = false, length = 32)
    private String webhookStatus;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_result", nullable = false, length = 32)
    private PaymentWebhookProcessingResult processingResult;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    public static PaymentWebhookEvent record(
            String providerEventId,
            Long paymentAttemptId,
            String webhookStatus,
            BigDecimal amount,
            PaymentWebhookProcessingResult processingResult) {

        if (providerEventId == null || providerEventId.isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "providerEventId is required");
        }

        if (paymentAttemptId == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "paymentAttemptId is required");
        }

        if (webhookStatus == null || webhookStatus.isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "webhookStatus is required");
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "amount must be positive");
        }

        if (processingResult == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "processingResult is required");
        }

        PaymentWebhookEvent event = new PaymentWebhookEvent();
        event.providerEventId = providerEventId;
        event.paymentAttemptId = paymentAttemptId;
        event.webhookStatus = webhookStatus;
        event.amount = amount.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        event.processingResult = processingResult;
        event.receivedAt = LocalDateTime.now();

        return event;
    }

    @PrePersist
    void prePersist() {
        if (receivedAt == null) {
            receivedAt = LocalDateTime.now();
        }
    }
}