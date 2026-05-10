package com.exequt.order.api;

import com.exequt.order.api.dto.OrderResponse;
import com.exequt.order.application.OrderCommandService;
import com.exequt.common.response.GenericResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private final OrderCommandService orderCommandService;

    public OrderController(OrderCommandService orderCommandService) {
        this.orderCommandService = orderCommandService;
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<GenericResponse<OrderResponse>> getOrder(@PathVariable Long orderId) {
        log.info("Fetching order orderId={}", orderId);
        OrderResponse body = orderCommandService.getOrder(orderId);
        return ResponseEntity.ok(GenericResponse.success(body));
    }
}
