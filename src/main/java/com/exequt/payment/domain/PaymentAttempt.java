package com.exequt.payment.domain;

import com.exequt.common.exception.BusinessException;
import com.exequt.common.exception.ConflictException;
import com.exequt.common.response.ResultCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "payment_attempts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentAttempt {

    private static final int MONEY_SCALE = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "provider_reference", nullable = false, length = 64)
    private String providerReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PaymentAttemptStatus status;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static PaymentAttempt createInitiated(Long orderId, BigDecimal amount, String providerReference) {

        if (orderId == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "orderId is required");
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "amount must be positive");
        }

        if (providerReference == null || providerReference.isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "providerReference is required");
        }

        PaymentAttempt attempt = new PaymentAttempt();
        attempt.orderId = orderId;
        attempt.amount = amount.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        attempt.providerReference = providerReference;
        attempt.status = PaymentAttemptStatus.INITIATED;

        return attempt;
    }

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();

        if (createdAt == null) {
            createdAt = now;
        }

        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean isActive() {
        return status == PaymentAttemptStatus.INITIATED;
    }

    public boolean isCompleted() {
        return status == PaymentAttemptStatus.CONFIRMED
                || status == PaymentAttemptStatus.FAILED;
    }

    public void confirm() {
        if (status != PaymentAttemptStatus.INITIATED) {
            throw new ConflictException("Only an initiated payment attempt can be confirmed");
        }

        this.status = PaymentAttemptStatus.CONFIRMED;
    }

    public void fail() {
        if (status != PaymentAttemptStatus.INITIATED) {
            throw new ConflictException("Only an initiated payment attempt can be failed");
        }

        this.status = PaymentAttemptStatus.FAILED;
    }
}