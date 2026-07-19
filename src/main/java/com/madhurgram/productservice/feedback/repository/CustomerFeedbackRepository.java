package com.madhurgram.productservice.feedback.repository;

import com.madhurgram.productservice.feedback.entity.CustomerFeedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CustomerFeedbackRepository extends JpaRepository<CustomerFeedback, Long> {

    List<CustomerFeedback> findAllByOrderByCreatedAtDesc();

    Page<CustomerFeedback> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<CustomerFeedback> findTop8ByRatingGreaterThanEqualAndIsApprovedTrueOrderByCreatedAtDesc(int rating);
}
