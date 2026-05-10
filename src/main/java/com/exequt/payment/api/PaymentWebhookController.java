package com.exequt.payment.api;

import com.exequt.common.response.GenericResponse;
import com.exequt.payment.api.dto.WebhookRequest;
import com.exequt.payment.api.dto.WebhookResponse;
import com.exequt.payment.application.PaymentApplicationService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments/mock-provider")
@Validated
public class PaymentWebhookController {

    private static final Logger log = LoggerFactory.getLogger(PaymentWebhookController.class);

    private final PaymentApplicationService paymentApplicationService;

    public PaymentWebhookController(PaymentApplicationService paymentApplicationService) {
        this.paymentApplicationService = paymentApplicationService;
    }

    @PostMapping("/webhook")
    public ResponseEntity<GenericResponse<WebhookResponse>> webhook(@Valid @RequestBody WebhookRequest request) {
        log.info(
                "Payment webhook received providerEventId={} paymentAttemptId={}",
                request.getProviderEventId(),
                request.getPaymentAttemptId());
        WebhookResponse body = paymentApplicationService.processWebhook(request);
        return ResponseEntity.ok(GenericResponse.success(body));
    }
}
