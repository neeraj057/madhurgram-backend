package com.madhurgram.productservice.marketing.controller;

import com.madhurgram.productservice.marketing.entity.ReviewRequest;
import com.madhurgram.productservice.marketing.repository.ReviewRequestRepository;
import com.madhurgram.productservice.marketing.scheduler.ReviewAutomationScheduler;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.Pattern;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Controller for tracking scheduler review queues and manual marketing outreach.
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/admin/marketing/reviews")
@Tag(name = "Admin — Reviews Automation", description = "Endpoints for managing auto-scheduler review requests and outbound links")
public class ReviewController {

    private final ReviewRequestRepository reviewRequestRepository;
    private final ReviewAutomationScheduler scheduler;

    /**
     * Constructor injection for ReviewController.
     *
     * @param reviewRequestRepository review requests database access
     * @param scheduler               automated task scheduler
     */
    public ReviewController(
            ReviewRequestRepository reviewRequestRepository,
            ReviewAutomationScheduler scheduler
    ) {
        this.reviewRequestRepository = reviewRequestRepository;
        this.scheduler = scheduler;
    }

    /**
     * Retrieves review scheduling queue entries.
     *
     * @return list of scheduled invites sorted by date descending
     */
    @GetMapping
    @Operation(summary = "Get review invite queue", description = "Retrieves a list of all automated review requests in chronological order.")
    public ResponseEntity<List<ReviewRequest>> getReviewQueue() {
        log.info("Admin request: fetch review invite queue");
        List<ReviewRequest> queue = reviewRequestRepository.findAll(Sort.by(Sort.Direction.DESC, "scheduledAt"));
        log.info("Returning {} queue entry/entries", queue.size());
        return ResponseEntity.ok(queue);
    }

    /**
     * Retrieves currently set redirection parameters for review automation.
     *
     * @return map with the configuration key/value pairs
     */
    @GetMapping("/config")
    @Operation(summary = "Get review configs", description = "Retrieves parameters like Google Maps profile URLs used in campaigns.")
    public ResponseEntity<Map<String, String>> getConfig() {
        log.info("Admin request: fetch review configuration");
        return ResponseEntity.ok(Map.of("googleReviewUrl", scheduler.getGoogleReviewUrl()));
    }

    /**
     * Modifies review configuration links.
     *
     * @param body map containing the target value configuration
     * @return updated configuration values
     */
    @PutMapping("/config")
    @Operation(summary = "Update review configs", description = "Configures targeting parameters like outbound Google Maps landing pages.")
    public ResponseEntity<Map<String, String>> updateConfig(@RequestBody Map<String, String> body) {
        String newUrl = body.get("googleReviewUrl");
        log.info("Admin request: update Google Review URL");
        
        if (newUrl != null && !newUrl.isBlank()) {
            scheduler.setGoogleReviewUrl(newUrl.trim());
            log.info("Google Review URL successfully updated");
        }
        
        return ResponseEntity.ok(Map.of("googleReviewUrl", scheduler.getGoogleReviewUrl()));
    }

    /**
     * Manually triggers an outbound review invite for a scheduled request immediately.
     *
     * @param id target scheduled review invite ID
     * @return status message or the request payload details
     */
    @PostMapping("/{id}/send-now")
    @Operation(summary = "Trigger review invite immediately", description = "Manually fires a WhatsApp/SMS review request immediately and skips schedule delays.")
    public ResponseEntity<?> sendNow(@PathVariable Long id) {
        log.info("Admin request: trigger review invite ID: {} immediately", id);
        
        ReviewRequest request = reviewRequestRepository.findById(id).orElse(null);
        if (request == null) {
            log.warn("Review trigger failed: request ID: {} not found", id);
            return ResponseEntity.notFound().build();
        }

        if ("SENT".equals(request.getStatus())) {
            log.warn("Review trigger rejected: request ID: {} has already been dispatched", id);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Review invitation has already been sent.");
        }

        try {
            scheduler.sendReviewInvite(request);
            log.info("Review invite ID: {} successfully sent immediately", id);
            return ResponseEntity.ok(request);
        } catch (Exception e) {
            log.error("Failed to send review invite ID: {} immediately", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    /**
     * Dispatches a manual test invite payload for verification.
     *
     * @param name  customer test name
     * @param phone customer validated phone number
     * @return saved request details
     */
    @PostMapping("/send-test")
    @Operation(summary = "Send test invite", description = "Fires a manual test notification invitation to verification numbers.")
    public ResponseEntity<?> sendTest(
            @RequestParam String name,
            @RequestParam
            @Pattern(regexp = "^(?:\\+91|91)?[6-9]\\d{9}$", message = "Invalid phone number format. Must be a valid 10-digit Indian mobile number optionally prefixed with +91 or 91.")
            String phone
    ) {
        log.info("Admin request: send test review invite to name='{}', phone='{}'", name, phone);

        if (name == null || name.isBlank()) {
            log.warn("Test invite aborted: customer name is mandatory");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Name is required.");
        }

        ReviewRequest request = ReviewRequest.builder()
                .orderId(0L) // Mock Order ID
                .customerName(name.trim())
                .customerPhone(phone.trim())
                .status("PENDING")
                .scheduledAt(LocalDateTime.now())
                .build();

        ReviewRequest saved = reviewRequestRepository.save(request);
        try {
            scheduler.sendReviewInvite(saved);
            log.info("Test review invite successfully dispatched to '{}'", phone);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            log.error("Failed to send test review invite to '{}'", phone, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}
