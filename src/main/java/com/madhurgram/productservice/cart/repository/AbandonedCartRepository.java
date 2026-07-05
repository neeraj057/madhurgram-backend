package com.madhurgram.productservice.cart.repository;

import com.madhurgram.productservice.cart.entity.AbandonedCart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AbandonedCartRepository extends JpaRepository<AbandonedCart, Long> {

    Optional<AbandonedCart> findByPhoneNumberAndIsRecoveredFalse(String phoneNumber);

    List<AbandonedCart> findByIsRecoveredFalseAndLastUpdatedBeforeOrderByLastUpdatedDesc(LocalDateTime cutoffTime);

    List<AbandonedCart> findByIsRecoveredFalseAndReminderSentFalseAndLastUpdatedBeforeOrderByLastUpdatedDesc(LocalDateTime cutoffTime);
}
