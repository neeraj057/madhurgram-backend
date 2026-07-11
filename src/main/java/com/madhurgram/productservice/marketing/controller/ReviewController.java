package com.madhurgram.productservice.marketing.controller;

import com.madhurgram.productservice.marketing.entity.ReviewRequest;
import com.madhurgram.productservice.marketing.repository.ReviewRequestRepository;
import com.madhurgram.productservice.marketing.scheduler.ReviewAutomationScheduler;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/marketing/reviews")
public class ReviewController {

    private final ReviewRequestRepository reviewRequestRepository;
    private final ReviewAutomationScheduler scheduler;

    public ReviewController(
            ReviewRequestRepository reviewRequestRepository,
            ReviewAutomationScheduler scheduler
    ) {
        this.reviewRequestRepository = reviewRequestRepository;
        this.scheduler = scheduler;
    }

    @GetMapping
    public ResponseEntity<List<ReviewRequest>> getReviewQueue() {
        return ResponseEntity.ok(reviewRequestRepository.findAll(Sort.by(Sort.Direction.DESC, "scheduledAt")));
    }

    @GetMapping("/config")
    public ResponseEntity<Map<String, String>> getConfig() {
        return ResponseEntity.ok(Map.of("googleReviewUrl", scheduler.getGoogleReviewUrl()));
    }

    @PutMapping("/config")
    public ResponseEntity<Map<String, String>> updateConfig(@RequestBody Map<String, String> body) {
        String newUrl = body.get("googleReviewUrl");
        if (newUrl != null && !newUrl.isBlank()) {
            scheduler.setGoogleReviewUrl(newUrl.trim());
        }
        return ResponseEntity.ok(Map.of("googleReviewUrl", scheduler.getGoogleReviewUrl()));
    }

    @PostMapping("/{id}/send-now")
    public ResponseEntity<?> sendNow(@PathVariable Long id) {
        ReviewRequest request = reviewRequestRepository.findById(id).orElse(null);
        if (request == null) {
            return ResponseEntity.notFound().build();
        }

        if ("SENT".equals(request.getStatus())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Review invitation has already been sent.");
        }

        try {
            scheduler.sendReviewInvite(request);
            return ResponseEntity.ok(request);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PostMapping("/send-test")
    public ResponseEntity<?> sendTest(
            @RequestParam String name,
            @RequestParam String phone
    ) {
        if (name == null || name.isBlank() || phone == null || phone.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Name and Phone number are required.");
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
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}
