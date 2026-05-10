package com.exequt.cart.application;

import com.exequt.order.api.dto.OrderResponse;
import lombok.Getter;

@Getter
public class CheckoutResult {

    private final OrderResponse order;
    private final boolean created;

    public CheckoutResult(OrderResponse order, boolean created) {
        this.order = order;
        this.created = created;
    }
}
