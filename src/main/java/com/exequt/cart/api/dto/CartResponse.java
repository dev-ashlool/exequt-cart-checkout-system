package com.exequt.cart.api.dto;

import com.exequt.cart.domain.CartStatus;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CartResponse {

    private final Long id;
    private final CartStatus status;
    private final List<CartItemResponse> items;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
}
