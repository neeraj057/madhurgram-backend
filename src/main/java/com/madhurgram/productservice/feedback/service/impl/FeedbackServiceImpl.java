package com.madhurgram.productservice.feedback.service.impl;

import com.madhurgram.productservice.feedback.dto.CustomerFeedbackDTO;
import com.madhurgram.productservice.feedback.entity.CustomerFeedback;
import com.madhurgram.productservice.feedback.mapper.FeedbackMapper;
import com.madhurgram.productservice.feedback.repository.CustomerFeedbackRepository;
import com.madhurgram.productservice.feedback.service.FeedbackService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FeedbackServiceImpl implements FeedbackService {

    private final CustomerFeedbackRepository feedbackRepository;
    private final FeedbackMapper feedbackMapper;

    public FeedbackServiceImpl(CustomerFeedbackRepository feedbackRepository, FeedbackMapper feedbackMapper) {
        this.feedbackRepository = feedbackRepository;
        this.feedbackMapper = feedbackMapper;
    }

    @Override
    @Transactional
    public CustomerFeedbackDTO submitFeedback(CustomerFeedbackDTO dto) {
        CustomerFeedback feedback = feedbackMapper.toEntity(dto);
        CustomerFeedback saved = feedbackRepository.save(feedback);
        return feedbackMapper.toDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerFeedbackDTO> getTestimonials() {
        List<CustomerFeedback> testimonials = feedbackRepository.findTop8ByRatingGreaterThanEqualOrderByCreatedAtDesc(4);
        return testimonials.stream()
                .map(feedbackMapper::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerFeedbackDTO> getFeedbacks() {
        List<CustomerFeedback> feedbacks = feedbackRepository.findAllByOrderByCreatedAtDesc();
        return feedbacks.stream()
                .map(feedbackMapper::toDTO)
                .toList();
    }
}
