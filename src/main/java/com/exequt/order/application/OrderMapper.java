package com.exequt.order.application;

import com.exequt.order.api.dto.OrderItemResponse;
import com.exequt.order.api.dto.OrderResponse;
import com.exequt.order.domain.Order;
import com.exequt.order.domain.OrderItem;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class OrderMapper {

    public OrderResponse toOrderResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .cartId(order.getCartId())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .items(mapItems(order))
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    public OrderItemResponse toOrderItemResponse(OrderItem item) {
        return OrderItemResponse.builder()
                .id(item.getId())
                .productId(item.getProductId())
                .quantity(item.getQuantity())
                .price(item.getPrice())
                .lineTotal(item.getLineTotal())
                .build();
    }

    private List<OrderItemResponse> mapItems(Order order) {
        return order.getItemsView().stream().map(this::toOrderItemResponse).toList();
    }
}
