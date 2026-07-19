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

    @org.springframework.data.jpa.repository.Query("SELECT c FROM CustomerFeedback c WHERE c.rating >= :minRating AND c.isApproved = true AND (c.sentiment IS NULL OR c.sentiment NOT IN :excludedSentiments) ORDER BY c.createdAt DESC")
    List<CustomerFeedback> findValidPublicTestimonials(@org.springframework.data.repository.query.Param("minRating") int minRating, @org.springframework.data.repository.query.Param("excludedSentiments") List<String> excludedSentiments, Pageable pageable);
}
