package com.exequt.order.domain;

import com.exequt.common.exception.BusinessException;
import com.exequt.common.exception.ConflictException;
import com.exequt.common.response.ResultCode;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity(name = "ExeQutOrder")
@Table(name = "orders")
@Access(AccessType.FIELD)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {

    private static final int MONEY_SCALE = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cart_id", nullable = false)
    private Long cartId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private OrderStatus status;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Getter(AccessLevel.NONE)
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderItem> items = new ArrayList<>();

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static Order createFromCartSnapshot(Long cartId, List<CartItemSnapshot> lines) {
        if (cartId == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Cart id is required");
        }
        if (lines == null || lines.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Cannot create order from empty cart snapshot");
        }

        Order order = new Order();
        order.cartId = cartId;
        order.status = OrderStatus.CREATED;
        order.items = new ArrayList<>();

        BigDecimal total = BigDecimal.ZERO;
        for (CartItemSnapshot line : lines) {
            OrderItem item = new OrderItem(order, line.productId(), line.quantity(), line.price());
            order.items.add(item);
            total = total.add(item.getLineTotal());
        }

        order.totalAmount = total.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        return order;
    }

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void startPayment() {
        if (status == OrderStatus.PAID) {
            throw new ConflictException("Order is already paid and cannot be changed");
        }
        if (status == OrderStatus.PENDING_PAYMENT) {
            throw new ConflictException("Order is already pending payment");
        }
        if (status == OrderStatus.CREATED || status == OrderStatus.PAYMENT_FAILED) {
            this.status = OrderStatus.PENDING_PAYMENT;
            return;
        }
        throw new ConflictException("Invalid order state for startPayment: " + status);
    }

    public void markPaid() {
        if (status == OrderStatus.PAID) {
            throw new ConflictException("Order is already paid");
        }
        if (status != OrderStatus.PENDING_PAYMENT) {
            throw new ConflictException("Order must be pending payment to mark as paid");
        }
        this.status = OrderStatus.PAID;
    }

    public void markPaymentFailed() {
        if (status == OrderStatus.PAID) {
            throw new ConflictException("Paid order cannot be marked as payment failed");
        }
        if (status != OrderStatus.PENDING_PAYMENT) {
            throw new ConflictException("Order must be pending payment to mark payment as failed");
        }
        this.status = OrderStatus.PAYMENT_FAILED;
    }

    public List<OrderItem> getItemsView() {
        return Collections.unmodifiableList(items);
    }
}