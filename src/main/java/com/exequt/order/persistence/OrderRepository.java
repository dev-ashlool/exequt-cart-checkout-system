package com.exequt.order.persistence;

import com.exequt.order.domain.Order;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByCartId(Long cartId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from ExeQutOrder o where o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") Long id);
}
