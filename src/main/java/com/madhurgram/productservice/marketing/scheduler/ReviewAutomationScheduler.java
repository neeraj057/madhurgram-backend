package com.madhurgram.productservice.marketing.scheduler;

import com.madhurgram.productservice.marketing.entity.ReviewRequest;
import com.madhurgram.productservice.marketing.repository.ReviewRequestRepository;
import com.madhurgram.productservice.marketing.service.SmsSenderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class ReviewAutomationScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReviewAutomationScheduler.class);

    private final ReviewRequestRepository reviewRequestRepository;
    private final SmsSenderService smsSenderService;

    @Value("${google.review.url:https://g.page/r/MadhurGramGhee/review}")
    private String googleReviewUrl;

    public ReviewAutomationScheduler(
            ReviewRequestRepository reviewRequestRepository,
            SmsSenderService smsSenderService
    ) {
        this.reviewRequestRepository = reviewRequestRepository;
        this.smsSenderService = smsSenderService;
    }

    public String getGoogleReviewUrl() {
        return googleReviewUrl;
    }

    public void setGoogleReviewUrl(String googleReviewUrl) {
        this.googleReviewUrl = googleReviewUrl;
    }

    @Scheduled(fixedDelay = 30000) // Scans every 30 seconds for pending invites
    @Transactional
    public void processScheduledReviews() {
        LocalDateTime now = LocalDateTime.now();
        List<ReviewRequest> pendingRequests = reviewRequestRepository.findByStatusAndScheduledAtBefore("PENDING", now);

        if (pendingRequests.isEmpty()) {
            return;
        }

        log.info("Google Review Automation: Found {} pending review invitations to send.", pendingRequests.size());

        for (ReviewRequest request : pendingRequests) {
            sendReviewInvite(request);
        }
    }

    public void sendReviewInvite(ReviewRequest request) {
        log.info("Sending scheduled review invitation to Customer: {}, Phone: {}", request.getCustomerName(), request.getCustomerPhone());

        String message = String.format(
                "नमस्ते %s, आपकी रसोई का 'MadhurGram' Ghee कैसा लगा? कृपया यहाँ अपना अनुभव साझा करें: %s",
                request.getCustomerName(), googleReviewUrl
        );

        try {
            int sent = smsSenderService.sendBroadcastMessage(List.of(request.getCustomerPhone()), message);
            if (sent > 0) {
                request.setStatus("SENT");
                request.setSentAt(LocalDateTime.now());
                log.info("Successfully sent Google Review invite to Customer ID: {}", request.getId());
            } else {
                request.setStatus("FAILED");
                log.warn("Twilio failed to transmit review invite to Customer ID: {}", request.getId());
            }
        } catch (Exception e) {
            request.setStatus("FAILED");
            log.error("Failed to execute review invitation delivery for ID: {}", request.getId(), e);
        }

        reviewRequestRepository.save(request);
    }
}
