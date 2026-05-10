package com.exequt.cart.api.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CartItemResponse {

    private final Long id;
    private final String productId;
    private final int quantity;
    private final BigDecimal price;
    private final BigDecimal lineTotal;
}
