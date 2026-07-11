package com.madhurgram.productservice.marketing.repository;

import com.madhurgram.productservice.marketing.entity.ReviewRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRequestRepository extends JpaRepository<ReviewRequest, Long> {
    List<ReviewRequest> findByStatus(String status);
    List<ReviewRequest> findByStatusAndScheduledAtBefore(String status, LocalDateTime time);
    Optional<ReviewRequest> findByOrderId(Long orderId);
}
