package com.madhurgram.productservice.feedback.service;

import com.madhurgram.productservice.feedback.dto.CustomerFeedbackDTO;
import java.util.List;

public interface FeedbackService {
    CustomerFeedbackDTO submitFeedback(CustomerFeedbackDTO dto);
    List<CustomerFeedbackDTO> getTestimonials();
    List<CustomerFeedbackDTO> getFeedbacks();
}
