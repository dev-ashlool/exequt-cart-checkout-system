package com.exequt.cart.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AddCartItemRequest {

    @NotBlank(message = "productId is required")
    private String productId;

    @NotNull(message = "quantity is required")
    @Positive(message = "quantity must be positive")
    private Integer quantity;

    @NotNull(message = "price is required")
    @Positive(message = "price must be positive")
    private BigDecimal price;
}
