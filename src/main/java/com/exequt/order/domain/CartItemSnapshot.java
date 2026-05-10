package com.exequt.order.domain;

import java.math.BigDecimal;

/**
 * Immutable line snapshot used when materializing an {@link Order} from a cart at checkout.
 */
public record CartItemSnapshot(String productId, int quantity, BigDecimal price) {}
