package com.exequt.payment.persistence;

import com.exequt.payment.domain.PaymentAttempt;
import com.exequt.payment.domain.PaymentAttemptStatus;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, Long> {

    Optional<PaymentAttempt> findByOrderIdAndStatus(Long orderId, PaymentAttemptStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PaymentAttempt p where p.id = :id")
    Optional<PaymentAttempt> findByIdForUpdate(@Param("id") Long id);
}
