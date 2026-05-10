package com.exequt.payment.api;

import com.exequt.common.response.GenericResponse;
import com.exequt.payment.api.dto.PaymentAttemptResponse;
import com.exequt.payment.application.PaymentApplicationService;
import com.exequt.payment.application.PaymentStartResult;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
public class OrderPaymentController {

    private static final Logger log = LoggerFactory.getLogger(OrderPaymentController.class);

    private final PaymentApplicationService paymentApplicationService;

    public OrderPaymentController(PaymentApplicationService paymentApplicationService) {
        this.paymentApplicationService = paymentApplicationService;
    }

    @PostMapping("/{orderId}/payment/start")
    public ResponseEntity<GenericResponse<PaymentAttemptResponse>> startPayment(@PathVariable Long orderId) {
        log.info("Payment start requested orderId={}", orderId);
        PaymentStartResult result = paymentApplicationService.startPayment(orderId);
        GenericResponse<PaymentAttemptResponse> body = GenericResponse.success(result.getAttempt());
        if (result.isCreated()) {
            URI location = URI.create("/orders/" + orderId + "/payment/attempts/" + result.getAttempt().getId());
            return ResponseEntity.created(location).body(body);
        }
        return ResponseEntity.ok(body);
    }
}
