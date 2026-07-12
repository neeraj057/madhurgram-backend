package com.madhurgram.productservice.feedback.service.impl;

import com.madhurgram.productservice.feedback.dto.CustomerFeedbackDTO;
import com.madhurgram.productservice.feedback.entity.CustomerFeedback;
import com.madhurgram.productservice.feedback.mapper.FeedbackMapper;
import com.madhurgram.productservice.feedback.repository.CustomerFeedbackRepository;
import com.madhurgram.productservice.feedback.service.FeedbackService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service implementation for managing customer testimonials, feedback submissions, 
 * ratings, and public review showcases.
 */
@Slf4j
@Service
public class FeedbackServiceImpl implements FeedbackService {

    private final CustomerFeedbackRepository feedbackRepository;
    private final FeedbackMapper feedbackMapper;

    /**
     * Constructor injection for FeedbackServiceImpl.
     *
     * @param feedbackRepository customer feedback repository
     * @param feedbackMapper     feedback mapper helper
     */
    public FeedbackServiceImpl(CustomerFeedbackRepository feedbackRepository, FeedbackMapper feedbackMapper) {
        this.feedbackRepository = feedbackRepository;
        this.feedbackMapper = feedbackMapper;
    }

    /**
     * Submits a customer feedback entry containing rating and sentiment.
     *
     * @param dto the customer feedback payload
     * @return the saved feedback details DTO
     */
    @Override
    @Transactional
    public CustomerFeedbackDTO submitFeedback(CustomerFeedbackDTO dto) {
        log.info("Submitting new customer feedback. Rating: {}, Sentiment: '{}'", dto.getRating(), dto.getSentiment());
        
        if (dto.getRating() == null || dto.getRating() < 1 || dto.getRating() > 5) {
            log.warn("Feedback submission failed: rating parameter must be between 1 and 5");
            throw new IllegalArgumentException("Rating must be a valid integer between 1 and 5.");
        }

        CustomerFeedback feedback = feedbackMapper.toEntity(dto);
        CustomerFeedback saved = feedbackRepository.save(feedback);
        log.info("Feedback entry successfully persisted with ID: {}", saved.getId());
        return feedbackMapper.toDTO(saved);
    }

    /**
     * Resolves positive testimonials to render on storefront homepages.
     *
     * @return a list of positive customer testimonials (4+ rating)
     */
    @Override
    @Transactional(readOnly = true)
    public List<CustomerFeedbackDTO> getTestimonials() {
        log.info("Retrieving top 8 positive testimonials with 4+ star ratings");
        List<CustomerFeedback> testimonials = feedbackRepository.findTop8ByRatingGreaterThanEqualOrderByCreatedAtDesc(4);
        return testimonials.stream()
                .map(feedbackMapper::toDTO)
                .toList();
    }

    /**
     * Lists all customer feedback submissions sorted chronologically descending.
     *
     * @return list of feedback entries
     */
    @Override
    @Transactional(readOnly = true)
    public List<CustomerFeedbackDTO> getFeedbacks() {
        log.info("Admin request: fetch all customer feedback records");
        List<CustomerFeedback> feedbacks = feedbackRepository.findAllByOrderByCreatedAtDesc();
        return feedbacks.stream()
                .map(feedbackMapper::toDTO)
                .toList();
    }
}
