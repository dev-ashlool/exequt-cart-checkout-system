package com.exequt.cart.domain;

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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "carts")
@Access(AccessType.FIELD)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CartStatus status;

    @Getter(AccessLevel.NONE)
    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<CartItem> items = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static Cart create() {
        Cart cart = new Cart();
        cart.status = CartStatus.OPEN;
        cart.items = new ArrayList<>();
        return cart;
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

    public void validateCanModify() {
        if (status != CartStatus.OPEN) {
            throw new ConflictException("Cart cannot be modified because it is not open");
        }
    }

    public void validateNotEmpty() {
        if (items == null || items.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Cart is empty");
        }
    }

    public void addItem(String productId, int quantity, BigDecimal price) {
        validateCanModify();
        if (productId == null || productId.isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "productId is required");
        }
        if (quantity <= 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "quantity must be positive");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "price must be positive");
        }

        Optional<CartItem> existing = items.stream()
                .filter(i -> i.getProductId().equals(productId))
                .findFirst();

        if (existing.isPresent()) {
            existing.get().mergeAdditional(quantity, price);
        } else {
            CartItem line = new CartItem(this, productId, quantity, price);
            items.add(line);
        }
        this.updatedAt = LocalDateTime.now();
    }

    public void checkout() {
        if (status == CartStatus.CHECKED_OUT) {
            throw new ConflictException("Cart is already checked out");
        }
        validateNotEmpty();
        this.status = CartStatus.CHECKED_OUT;
    }

    public List<CartItem> getItemsView() {
        return Collections.unmodifiableList(items);
    }
}
