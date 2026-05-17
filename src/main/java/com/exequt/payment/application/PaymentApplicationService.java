package com.exequt.payment.application;

import com.exequt.common.exception.BusinessException;
import com.exequt.common.exception.NotFoundException;
import com.exequt.common.response.ResultCode;
import com.exequt.order.api.dto.OrderResponse;
import com.exequt.order.application.OrderCommandService;
import com.exequt.payment.api.dto.WebhookRequest;
import com.exequt.payment.api.dto.WebhookResponse;
import com.exequt.payment.domain.PaymentAttempt;
import com.exequt.payment.domain.PaymentAttemptStatus;
import com.exequt.payment.domain.PaymentWebhookEvent;
import com.exequt.payment.domain.PaymentWebhookProcessingResult;
import com.exequt.payment.domain.WebhookProviderStatus;
import com.exequt.payment.persistence.PaymentAttemptRepository;
import com.exequt.payment.persistence.PaymentWebhookEventRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentApplicationService {

    private static final int MONEY_SCALE = 2;

    private final OrderCommandService orderCommandService;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final PaymentWebhookEventRepository paymentWebhookEventRepository;
    private final PaymentMapper paymentMapper;

    public PaymentApplicationService(
            OrderCommandService orderCommandService,
            PaymentAttemptRepository paymentAttemptRepository,
            PaymentWebhookEventRepository paymentWebhookEventRepository,
            PaymentMapper paymentMapper) {
        this.orderCommandService = orderCommandService;
        this.paymentAttemptRepository = paymentAttemptRepository;
        this.paymentWebhookEventRepository = paymentWebhookEventRepository;
        this.paymentMapper = paymentMapper;
    }

@Transactional
public PaymentStartResult startPayment(Long orderId) {
    Optional<PaymentAttempt> active =
            paymentAttemptRepository.findByOrderIdAndStatus(orderId, PaymentAttemptStatus.INITIATED);

    if (active.isPresent()) {
        return new PaymentStartResult(paymentMapper.toPaymentAttemptResponse(active.get()), false);
    }

    orderCommandService.ensureOrderReadyForNewPaymentAttempt(orderId);

    OrderResponse order = orderCommandService.getOrder(orderId);
    String providerReference = UUID.randomUUID().toString();

    PaymentAttempt attempt = PaymentAttempt.createInitiated(
            orderId,
            order.getTotalAmount(),
            providerReference
    );

    PaymentAttempt saved = paymentAttemptRepository.save(attempt);
    return new PaymentStartResult(paymentMapper.toPaymentAttemptResponse(saved), true);
}

    @Transactional
    public WebhookResponse processWebhook(WebhookRequest request) {
        String providerEventId = request.getProviderEventId();
        if (providerEventId == null || providerEventId.isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "providerEventId is required");
        }

        //Check-then-lock
        if (paymentWebhookEventRepository.existsByProviderEventId(providerEventId)) {
            return WebhookResponse.duplicate();
        }

        PaymentAttempt attempt = paymentAttemptRepository
                .findByIdForUpdate(request.getPaymentAttemptId())
                .orElseThrow(() -> new NotFoundException("Payment attempt not found"));

        //Double-checked idempotency validation
        if (paymentWebhookEventRepository.existsByProviderEventId(providerEventId)) {
            return WebhookResponse.duplicate();
        }

        BigDecimal webhookAmount = request.getAmount().setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        if (attempt.getAmount().compareTo(webhookAmount) != 0) {
            PaymentWebhookEvent rejected = PaymentWebhookEvent.recordEvent(
                    providerEventId,
                    attempt.getId(),
                    request.getStatus().name(),
                    webhookAmount,
                    PaymentWebhookProcessingResult.REJECTED);
            paymentWebhookEventRepository.save(rejected);
            return WebhookResponse.rejected("Amount does not match payment attempt");
        }

        WebhookProviderStatus terminal = request.getStatus();

        if (attempt.isCompleted()) {
            PaymentWebhookEvent ignored = PaymentWebhookEvent.recordEvent(
                    providerEventId,
                    attempt.getId(),
                    terminal.name(),
                    webhookAmount,
                    PaymentWebhookProcessingResult.IGNORED);
            paymentWebhookEventRepository.save(ignored);
            return WebhookResponse.ignored();
        }

        if (terminal == WebhookProviderStatus.CONFIRMED) {
            attempt.confirm();
            orderCommandService.markOrderAsPaid(attempt.getOrderId());
        } else {
            attempt.fail();
            orderCommandService.markOrderPaymentFailed(attempt.getOrderId());
        }
        paymentAttemptRepository.save(attempt);

        PaymentWebhookEvent processed = PaymentWebhookEvent.recordEvent(
                providerEventId,
                attempt.getId(),
                terminal.name(),
                webhookAmount,
                PaymentWebhookProcessingResult.PROCESSED);
        paymentWebhookEventRepository.save(processed);
        return WebhookResponse.processed();
    }
}
