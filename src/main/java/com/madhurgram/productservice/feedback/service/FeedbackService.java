package com.madhurgram.productservice.feedback.service;

import com.madhurgram.productservice.feedback.dto.CustomerFeedbackDTO;
import java.util.List;

/**
 * Service interface managing customer feedback, ratings, public home testimonials, 
 * and context-specific suggested feedback comments generation.
 */
public interface FeedbackService {
    
    CustomerFeedbackDTO submitFeedback(CustomerFeedbackDTO dto);
    
    List<CustomerFeedbackDTO> getTestimonials();
    
    List<CustomerFeedbackDTO> getFeedbacks();

    List<String> getFeedbackSuggestions(Long orderId);

    CustomerFeedbackDTO approveFeedback(Long id);

    void deleteFeedback(Long id);
}
