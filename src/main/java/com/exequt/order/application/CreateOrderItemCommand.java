package com.exequt.order.application;

import java.math.BigDecimal;

/**
 * Line snapshot passed into {@link OrderCommandService} when creating an order from a cart.
 */
public record CreateOrderItemCommand(String productId, int quantity, BigDecimal price) {}
