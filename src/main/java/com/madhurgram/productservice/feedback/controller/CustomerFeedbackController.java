package com.madhurgram.productservice.feedback.controller;

import com.madhurgram.productservice.feedback.dto.CustomerFeedbackDTO;
import com.madhurgram.productservice.feedback.service.FeedbackService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controller for managing buyer testimonials, ratings, and feedback uploads.
 */
@Slf4j
@RestController
@RequestMapping("/api")
@Tag(name = "Customer Feedback", description = "Endpoints for submitting and retrieving reviews, testimonials, and feedback images")
public class CustomerFeedbackController {

    private static final String UPLOAD_DIR_PATH = "uploads/feedback";
    private static final String DOT = ".";
    private static final String ERROR_KEY = "error";
    private static final String URL_KEY = "url";
    private static final String UPLOAD_SUBPATH = "/uploads/feedback/";

    private final FeedbackService feedbackService;

    @Value("${madhurgram.app.backend-url:http://localhost:8080}")
    private String backendUrl;

    /**
     * Constructor injection for CustomerFeedbackController.
     *
     * @param feedbackService feedback management service
     */
    public CustomerFeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    /**
     * Submits customer feedback.
     *
     * @param dto feedback details payload DTO
     * @return the saved feedback details
     */
    @PostMapping("/public/feedback")
    @Operation(summary = "Submit new feedback", description = "Allows a customer to submit their rating, comments, and order references.")
    public ResponseEntity<CustomerFeedbackDTO> submitFeedback(@RequestBody CustomerFeedbackDTO dto) {
        log.info("Feedback submission request: Rating={}, Order ID={}", dto.getRating(), dto.getOrderId());
        CustomerFeedbackDTO saved = feedbackService.submitFeedback(dto);
        log.info("Feedback successfully saved with ID: {}", saved.getId());
        return ResponseEntity.ok(saved);
    }

    /**
     * Generates a list of suggested feedback comments based on the items in a given order.
     *
     * @param orderId optional order ID to analyze
     * @return suggested feedback statements
     */
    @GetMapping("/public/feedback/suggestions")
    @Operation(summary = "Get suggested feedback statements", description = "Analyzes items in an order and suggests context-specific review comments.")
    public ResponseEntity<List<String>> getFeedbackSuggestions(@RequestParam(required = false) Long orderId) {
        log.info("Request suggestions for order ID: {}", orderId);
        List<String> suggestions = feedbackService.getFeedbackSuggestions(orderId);
        return ResponseEntity.ok(suggestions);
    }

    /**
     * Uploads a customer feedback picture and saves it locally.
     *
     * @param file the multipart image file to upload
     * @return map with image file URL
     */
    @PostMapping("/public/feedback/upload")
    @Operation(summary = "Upload review image", description = "Uploads a photo submitted by a customer for their review.")
    public ResponseEntity<Map<String, String>> uploadFeedbackImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            log.warn("Feedback image upload failed: file is empty");
            return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "File is empty"));
        }
        try {
            String originalFileName = file.getOriginalFilename();
            String extension = "";
            if (originalFileName != null && originalFileName.contains(DOT)) {
                extension = originalFileName.substring(originalFileName.lastIndexOf(DOT));
            }
            String fileName = UUID.randomUUID().toString() + extension;

            File uploadDir = new File(UPLOAD_DIR_PATH);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            File destination = new File(uploadDir.getAbsolutePath(), fileName);
            Files.copy(
                file.getInputStream(), 
                destination.toPath(), 
                StandardCopyOption.REPLACE_EXISTING
            );

            // Resolved hardcoded localhost URL: read backendUrl property dynamically
            String fileUrl = backendUrl.trim() + UPLOAD_SUBPATH + fileName;
            log.info("Feedback image successfully uploaded: '{}'", fileUrl);
            return ResponseEntity.ok(Map.of(URL_KEY, fileUrl));
        } catch (Exception e) {
            log.error("Failed to upload feedback image", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(ERROR_KEY, "Failed to upload file"));
        }
    }

    /**
     * Retrieves positive testimonials to showcase on public homepages.
     *
     * @return top positive feedbacks (rating >= 4)
     */
    @GetMapping("/public/feedback/testimonials")
    @Operation(summary = "Get storefront testimonials", description = "Retrieves the top 8 recent customer feedbacks with 4+ star ratings.")
    public ResponseEntity<List<CustomerFeedbackDTO>> getTestimonials() {
        log.info("Request: fetch public testimonials");
        List<CustomerFeedbackDTO> dtos = feedbackService.getTestimonials();
        log.info("Returning {} testimonials", dtos.size());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Retrieves all customer feedback submissions for the admin dashboard.
     *
     * @return list of feedbacks
     */
    @GetMapping("/admin/feedback")
    @Operation(summary = "List all feedbacks (Admin)", description = "Retrieves all feedback submissions sorted by creation date descending.")
    public ResponseEntity<List<CustomerFeedbackDTO>> getFeedbacks() {
        log.info("Admin request: list all feedbacks");
        List<CustomerFeedbackDTO> dtos = feedbackService.getFeedbacks();
        log.info("Returning {} feedbacks to admin", dtos.size());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Approves a customer feedback.
     *
     * @param id target feedback ID
     * @return the approved feedback DTO
     */
    @PutMapping("/admin/feedback/{id}/approve")
    @Operation(summary = "Approve feedback (Admin)", description = "Marks a customer feedback as approved and visible on storefront testimonials.")
    public ResponseEntity<CustomerFeedbackDTO> approveFeedback(@PathVariable Long id) {
        log.info("Admin request: approve feedback ID: {}", id);
        CustomerFeedbackDTO approved = feedbackService.approveFeedback(id);
        return ResponseEntity.ok(approved);
    }

    /**
     * Deletes / rejects a customer feedback.
     *
     * @param id target feedback ID
     * @return HTTP status 204 (No Content)
     */
    @DeleteMapping("/admin/feedback/{id}")
    @Operation(summary = "Delete / Reject feedback (Admin)", description = "Permanently deletes a feedback submission.")
    public ResponseEntity<Void> deleteFeedback(@PathVariable Long id) {
        log.info("Admin request: reject/delete feedback ID: {}", id);
        feedbackService.deleteFeedback(id);
        return ResponseEntity.noContent().build();
    }
}
