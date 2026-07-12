package com.madhurgram.productservice.marketing.service.impl;

import com.madhurgram.productservice.marketing.dto.ReviewRequestDTO;
import com.madhurgram.productservice.marketing.entity.ReviewRequest;
import com.madhurgram.productservice.marketing.mapper.ReviewMapper;
import com.madhurgram.productservice.marketing.repository.ReviewRequestRepository;
import com.madhurgram.productservice.marketing.scheduler.ReviewAutomationScheduler;
import com.madhurgram.productservice.marketing.service.ReviewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Service implementation for administering the automated reviews queue, 
 * scheduler dispatches, and WhatsApp outreach triggers.
 */
@Slf4j
@Service
public class ReviewServiceImpl implements ReviewService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_SENT = "SENT";
    private static final String SORT_FIELD_SCHEDULED = "scheduledAt";

    // Validates Indian phone numbers: optional country code (+91/91) followed by 10 digits starting with 6-9
    private static final Pattern PHONE_PATTERN = Pattern.compile("^(?:\\+91|91)?[6-9]\\d{9}$");

    private final ReviewRequestRepository reviewRequestRepository;
    private final ReviewAutomationScheduler scheduler;
    private final ReviewMapper reviewMapper;

    /**
     * Constructor injection for ReviewServiceImpl.
     *
     * @param reviewRequestRepository review requests queue repository
     * @param scheduler               automation worker scheduler
     * @param reviewMapper            review request mapper
     */
    public ReviewServiceImpl(ReviewRequestRepository reviewRequestRepository,
                             ReviewAutomationScheduler scheduler,
                             ReviewMapper reviewMapper) {
        this.reviewRequestRepository = reviewRequestRepository;
        this.scheduler = scheduler;
        this.reviewMapper = reviewMapper;
    }

    /**
     * Lists review scheduling queue entries sorted chronologically descending.
     *
     * @return list of scheduled review requests DTO
     */
    @Override
    @Transactional(readOnly = true)
    public List<ReviewRequestDTO> getReviewQueue() {
        log.info("Admin request: fetch review invite queue");
        List<ReviewRequest> queue = reviewRequestRepository.findAll(Sort.by(Sort.Direction.DESC, SORT_FIELD_SCHEDULED));
        return queue.stream()
                .map(reviewMapper::toDTO)
                .toList();
    }

    /**
     * Manually triggers an outbound review invite immediately, skipping scheduler delay limits.
     *
     * @param id target scheduled review invite ID
     * @return updated review request details DTO
     */
    @Override
    @Transactional
    public ReviewRequestDTO sendNow(Long id) {
        log.info("Manually sending review request immediately for ID: {}", id);
        
        if (id == null) {
            throw new IllegalArgumentException("Review invite ID must not be null.");
        }

        ReviewRequest request = reviewRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Review invite request ID not found: " + id));

        if (STATUS_SENT.equals(request.getStatus())) {
            log.warn("Send immediately rejected: Review invite ID: {} already sent", id);
            throw new IllegalArgumentException("Review invitation has already been sent.");
        }

        try {
            scheduler.sendReviewInvite(request);
            log.info("Successfully dispatched review request ID: {}", id);
            return reviewMapper.toDTO(request);
        } catch (Exception e) {
            log.error("Failed to send review invite ID: {} immediately", id, e);
            throw new RuntimeException("Failed to send review invite ID " + id + ": " + e.getMessage(), e);
        }
    }

    /**
     * Dispatches a manual test invite to check message formats.
     *
     * @param name  customer test name
     * @param phone customer target phone number
     * @return saved review request details DTO
     */
    @Override
    @Transactional
    public ReviewRequestDTO sendTest(String name, String phone) {
        log.info("Sending test review invitation to phone: '{}', name: '{}'", phone, name);
        
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name is required.");
        }
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("Phone number is required.");
        }

        String cleanPhone = phone.trim();
        String cleanName = name.trim();

        // Perform strict Indian phone validation to reject invalid formats before Twilio API dispatch
        if (!PHONE_PATTERN.matcher(cleanPhone).matches()) {
            log.warn("Test invitation rejected: phone number format is invalid: '{}'", cleanPhone);
            throw new IllegalArgumentException("Invalid Indian mobile number format. Must match standard +91 or 10-digit formats.");
        }

        ReviewRequest request = ReviewRequest.builder()
                .orderId(0L)
                .customerName(cleanName)
                .customerPhone(cleanPhone)
                .status(STATUS_PENDING)
                .scheduledAt(LocalDateTime.now())
                .build();

        ReviewRequest saved = reviewRequestRepository.save(request);
        try {
            scheduler.sendReviewInvite(saved);
            log.info("Successfully dispatched test review invitation to phone: '{}'", cleanPhone);
            return reviewMapper.toDTO(saved);
        } catch (Exception e) {
            log.error("Failed to send test review invite to phone: '{}'", cleanPhone, e);
            throw new RuntimeException("Failed to send test review invite: " + e.getMessage(), e);
        }
    }
}
