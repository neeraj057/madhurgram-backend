package com.madhurgram.productservice.feedback.service.impl;

import com.madhurgram.productservice.common.entity.SystemSetting;
import com.madhurgram.productservice.common.repository.SystemSettingRepository;
import com.madhurgram.productservice.feedback.dto.CustomerFeedbackDTO;
import com.madhurgram.productservice.feedback.entity.CustomerFeedback;
import com.madhurgram.productservice.feedback.mapper.FeedbackMapper;
import com.madhurgram.productservice.feedback.repository.CustomerFeedbackRepository;
import com.madhurgram.productservice.feedback.service.FeedbackService;
import com.madhurgram.productservice.order.entity.Order;
import com.madhurgram.productservice.order.entity.OrderItem;
import com.madhurgram.productservice.order.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service implementation for managing customer testimonials, feedback submissions, 
 * ratings, and dynamic feedback suggestion generation based on past purchase items.
 */
@Slf4j
@Service
public class FeedbackServiceImpl implements FeedbackService {

    private static final String PRODUCT_GHEE = "ghee";
    private static final String PRODUCT_OIL = "oil";
    private static final String PRODUCT_TEL = "tel";
    private static final String PRODUCT_HONEY = "honey";
    private static final String PRODUCT_SHAHAD = "shahad";
    private static final String PRODUCT_SWEET = "sweet";
    private static final String PRODUCT_MITHAI = "mithai";
    private static final String PRODUCT_PEDA = "peda";
    private static final String PRODUCT_LADDU = "laddu";

    // Java-level constant defaults to keep properties clean
    private static final List<String> DEFAULT_GHEE_SUGGESTIONS = List.of(
            "Desi Ghee ka swad sach mein lajawab aur shuddh hai! 💛",
            "Ghee ki dhoop jaisi khushboo ne dil jeet liya."
    );
    private static final List<String> DEFAULT_OIL_SUGGESTIONS = List.of(
            "Kachchi ghani tel ki shuddhata 100% genuine hai.",
            "Tel ki packaging leak-proof aur secure thi."
    );
    private static final List<String> DEFAULT_HONEY_SUGGESTIONS = List.of(
            "Pure honey ki mithas aur swad lajawab hai! 🍯"
    );
    private static final List<String> DEFAULT_SWEETS_SUGGESTIONS = List.of(
            "Mithai ka swad bilkul shuddh desi ghee jaisa hai!",
            "Mithai bohot tazi aur naram thi."
    );
    private static final List<String> DEFAULT_GENERIC_SUGGESTIONS = List.of(
            "Delivery bilkul sahi samay par hui. 🚚",
            "MadhurGram ke products ka swad bilkul gaanv jaisa authentic hai! ✨",
            "Packaging bohot surakshit aur clean thi.",
            "Customer support aur ordering experience bohot smooth tha."
    );

    private final CustomerFeedbackRepository feedbackRepository;
    private final FeedbackMapper feedbackMapper;
    private final OrderRepository orderRepository;
    private final SystemSettingRepository systemSettingRepository;

    /**
     * Constructor injection for FeedbackServiceImpl.
     *
     * @param feedbackRepository       customer feedback repository
     * @param feedbackMapper           feedback mapper helper
     * @param orderRepository          order database repository
     * @param systemSettingRepository  global system settings repository
     */
    public FeedbackServiceImpl(CustomerFeedbackRepository feedbackRepository, 
                               FeedbackMapper feedbackMapper,
                               OrderRepository orderRepository,
                               SystemSettingRepository systemSettingRepository) {
        this.feedbackRepository = feedbackRepository;
        this.feedbackMapper = feedbackMapper;
        this.orderRepository = orderRepository;
        this.systemSettingRepository = systemSettingRepository;
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
        if (dto.getOrderId() != null) {
            feedback.setIsApproved(true);
        } else {
            feedback.setIsApproved(false);
        }
        CustomerFeedback saved = feedbackRepository.save(feedback);
        log.info("Feedback entry successfully persisted with ID: {} (Approved: {})", saved.getId(), saved.getIsApproved());
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
        List<CustomerFeedback> testimonials = feedbackRepository
                .findTop8ByRatingGreaterThanEqualAndIsApprovedTrueOrderByCreatedAtDesc(4);
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

    /**
     * Generates a list of suggested feedback comments based on the items in a given order.
     *
     * @param orderId optional order ID to analyze
     * @return suggested feedback statements
     */
    @Override
    @Transactional(readOnly = true)
    @org.springframework.cache.annotation.Cacheable(value = "feedback_suggestions", key = "#orderId", unless = "#result == null")
    public List<String> getFeedbackSuggestions(Long orderId) {
        log.info("Generating feedback suggestions for order ID: {}", orderId);
        List<String> suggestions = new ArrayList<>();

        if (orderId != null) {
            Optional<Order> orderOpt = orderRepository.findById(orderId);
            if (orderOpt.isPresent()) {
                Order order = orderOpt.get();
                boolean hasGhee = false;
                boolean hasOil = false;
                boolean hasHoney = false;
                boolean hasSweets = false;

                if (order.getOrderItems() != null) {
                    for (OrderItem item : order.getOrderItems()) {
                        if (item.getProductName() == null) {
                            continue;
                        }
                        String name = item.getProductName().toLowerCase();
                        if (name.contains(PRODUCT_GHEE)) {
                            hasGhee = true;
                        }
                        if (name.contains(PRODUCT_OIL) || name.contains(PRODUCT_TEL)) {
                            hasOil = true;
                        }
                        if (name.contains(PRODUCT_HONEY) || name.contains(PRODUCT_SHAHAD)) {
                            hasHoney = true;
                        }
                        if (name.contains(PRODUCT_SWEET) || name.contains(PRODUCT_MITHAI) ||
                                name.contains(PRODUCT_PEDA) || name.contains(PRODUCT_LADDU)) {
                            hasSweets = true;
                        }
                    }
                }

                if (hasGhee) {
                    suggestions.addAll(getSuggestionsList("FEEDBACK_SUGGESTIONS_GHEE", DEFAULT_GHEE_SUGGESTIONS));
                }
                if (hasOil) {
                    suggestions.addAll(getSuggestionsList("FEEDBACK_SUGGESTIONS_OIL", DEFAULT_OIL_SUGGESTIONS));
                }
                if (hasHoney) {
                    suggestions.addAll(getSuggestionsList("FEEDBACK_SUGGESTIONS_HONEY", DEFAULT_HONEY_SUGGESTIONS));
                }
                if (hasSweets) {
                    suggestions.addAll(getSuggestionsList("FEEDBACK_SUGGESTIONS_SWEETS", DEFAULT_SWEETS_SUGGESTIONS));
                }
            }
        }

        // Add default/generic suggestions if list is small to ensure premium options
        if (suggestions.size() < 4) {
            suggestions.addAll(getSuggestionsList("FEEDBACK_SUGGESTIONS_GENERIC", DEFAULT_GENERIC_SUGGESTIONS));
        }

        return suggestions;
    }

    private List<String> getSuggestionsList(String settingKey, List<String> fallback) {
        List<String> defaultList = fallback != null ? fallback : List.of();
        return systemSettingRepository.findById(settingKey)
                .map(setting -> {
                    if (setting.getSettingValue() == null || setting.getSettingValue().trim().isEmpty()) {
                        return defaultList;
                    }
                    return java.util.Arrays.stream(setting.getSettingValue().split(";"))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .toList();
                })
                .orElse(defaultList);
    }

    @Override
    @Transactional
    public CustomerFeedbackDTO approveFeedback(Long id) {
        log.info("Approving customer feedback ID: {}", id);
        CustomerFeedback feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Feedback not found with ID: " + id));
        feedback.setIsApproved(true);
        CustomerFeedback saved = feedbackRepository.save(feedback);
        return feedbackMapper.toDTO(saved);
    }

    @Override
    @Transactional
    public void deleteFeedback(Long id) {
        log.info("Deleting/Rejecting customer feedback ID: {}", id);
        if (!feedbackRepository.existsById(id)) {
            throw new IllegalArgumentException("Feedback not found with ID: " + id);
        }
        feedbackRepository.deleteById(id);
    }
}
