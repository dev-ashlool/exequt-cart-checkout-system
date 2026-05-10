package com.exequt.cart.domain;

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
@Table(name = "cart_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CartItem {

    private static final int MONEY_SCALE = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @Column(name = "product_id", nullable = false, length = 255)
    private String productId;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    @Column(name = "line_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal lineTotal;

    CartItem(Cart cart, String productId, int quantity, BigDecimal price) {
        this.cart = cart;
        this.productId = productId;
        this.quantity = quantity;
        this.price = price.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        this.lineTotal = computeLineTotal();
    }

    void mergeAdditional(int additionalQuantity, BigDecimal unitPrice) {
        this.quantity = this.quantity + additionalQuantity;
        this.price = unitPrice.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        this.lineTotal = computeLineTotal();
    }

    private BigDecimal computeLineTotal() {
        return price.multiply(BigDecimal.valueOf(quantity)).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
