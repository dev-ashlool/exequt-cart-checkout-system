package com.exequt.order.api.dto;

import com.exequt.order.domain.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderResponse {

    private final Long id;
    private final Long cartId;
    private final OrderStatus status;
    private final BigDecimal totalAmount;
    private final List<OrderItemResponse> items;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
}
