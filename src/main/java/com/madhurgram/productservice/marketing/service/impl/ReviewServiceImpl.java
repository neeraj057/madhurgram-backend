package com.madhurgram.productservice.marketing.service.impl;

import com.madhurgram.productservice.marketing.dto.ReviewRequestDTO;
import com.madhurgram.productservice.marketing.entity.ReviewRequest;
import com.madhurgram.productservice.marketing.mapper.ReviewMapper;
import com.madhurgram.productservice.marketing.repository.ReviewRequestRepository;
import com.madhurgram.productservice.marketing.scheduler.ReviewAutomationScheduler;
import com.madhurgram.productservice.marketing.service.ReviewService;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRequestRepository reviewRequestRepository;
    private final ReviewAutomationScheduler scheduler;
    private final ReviewMapper reviewMapper;

    public ReviewServiceImpl(ReviewRequestRepository reviewRequestRepository,
                             ReviewAutomationScheduler scheduler,
                             ReviewMapper reviewMapper) {
        this.reviewRequestRepository = reviewRequestRepository;
        this.scheduler = scheduler;
        this.reviewMapper = reviewMapper;
    }

    @Override
    public List<ReviewRequestDTO> getReviewQueue() {
        List<ReviewRequest> queue = reviewRequestRepository.findAll(Sort.by(Sort.Direction.DESC, "scheduledAt"));
        return queue.stream()
                .map(reviewMapper::toDTO)
                .toList();
    }

    @Override
    @Transactional
    public ReviewRequestDTO sendNow(Long id) {
        ReviewRequest request = reviewRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Review invite request ID not found: " + id));

        if ("SENT".equals(request.getStatus())) {
            throw new IllegalArgumentException("Review invitation has already been sent.");
        }

        try {
            scheduler.sendReviewInvite(request);
            return reviewMapper.toDTO(request);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send review invite ID " + id + ": " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public ReviewRequestDTO sendTest(String name, String phone) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name is required.");
        }

        ReviewRequest request = ReviewRequest.builder()
                .orderId(0L)
                .customerName(name.trim())
                .customerPhone(phone.trim())
                .status("PENDING")
                .scheduledAt(LocalDateTime.now())
                .build();

        ReviewRequest saved = reviewRequestRepository.save(request);
        try {
            scheduler.sendReviewInvite(saved);
            return reviewMapper.toDTO(saved);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send test review invite: " + e.getMessage(), e);
        }
    }
}
