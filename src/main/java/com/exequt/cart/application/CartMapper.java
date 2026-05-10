package com.exequt.cart.application;

import com.exequt.cart.api.dto.CartItemResponse;
import com.exequt.cart.api.dto.CartResponse;
import com.exequt.cart.api.dto.CreateCartResponse;
import com.exequt.cart.domain.Cart;
import com.exequt.cart.domain.CartItem;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CartMapper {

    public CreateCartResponse toCreateCartResponse(Cart cart) {
        return CreateCartResponse.builder()
                .id(cart.getId())
                .status(cart.getStatus())
                .items(mapItems(cart))
                .createdAt(cart.getCreatedAt())
                .updatedAt(cart.getUpdatedAt())
                .build();
    }

    public CartResponse toCartResponse(Cart cart) {
        return CartResponse.builder()
                .id(cart.getId())
                .status(cart.getStatus())
                .items(mapItems(cart))
                .createdAt(cart.getCreatedAt())
                .updatedAt(cart.getUpdatedAt())
                .build();
    }

    public CartItemResponse toCartItemResponse(CartItem item) {
        return CartItemResponse.builder()
                .id(item.getId())
                .productId(item.getProductId())
                .quantity(item.getQuantity())
                .price(item.getPrice())
                .lineTotal(item.getLineTotal())
                .build();
    }

    private List<CartItemResponse> mapItems(Cart cart) {
        return cart.getItemsView().stream().map(this::toCartItemResponse).toList();
    }
}
