package com.exequt.order.domain;

import com.exequt.common.exception.BusinessException;
import com.exequt.common.response.ResultCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "order_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem {

    private static final int MONEY_SCALE = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "product_id", nullable = false, length = 255)
    private String productId;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    @Column(name = "line_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal lineTotal;

    OrderItem(Order order, String productId, int quantity, BigDecimal price) {

        if (order == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Order is required");
        }

        if (productId == null || productId.isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Product id is required");
        }

        if (quantity <= 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Quantity must be positive");
        }

        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Price must be positive");
        }

        this.order = order;
        this.productId = productId;
        this.quantity = quantity;
        this.price = price.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        this.lineTotal = computeLineTotal();
    }

    private BigDecimal computeLineTotal() {
        return price
                .multiply(BigDecimal.valueOf(quantity))
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}