package com.exequt.payment.persistence;

import com.exequt.payment.domain.PaymentWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentWebhookEventRepository extends JpaRepository<PaymentWebhookEvent, Long> {

    boolean existsByProviderEventId(String providerEventId);
}
