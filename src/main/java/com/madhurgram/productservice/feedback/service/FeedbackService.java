package com.madhurgram.productservice.feedback.service;

import com.madhurgram.productservice.feedback.dto.CustomerFeedbackDTO;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface managing customer feedback, ratings, public home testimonials, 
 * and context-specific suggested feedback comments generation.
 */
public interface FeedbackService {
    
    CustomerFeedbackDTO submitFeedback(CustomerFeedbackDTO dto);
    
    List<CustomerFeedbackDTO> getTestimonials();
    
    List<CustomerFeedbackDTO> getFeedbacks();

    Page<CustomerFeedbackDTO> getFeedbacks(Pageable pageable);

    List<String> getFeedbackSuggestions(Long orderId);

    CustomerFeedbackDTO approveFeedback(Long id);

    void deleteFeedback(Long id);
}
