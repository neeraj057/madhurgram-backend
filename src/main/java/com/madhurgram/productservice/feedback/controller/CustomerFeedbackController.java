package com.madhurgram.productservice.feedback.controller;

import com.madhurgram.productservice.feedback.entity.CustomerFeedback;
import com.madhurgram.productservice.feedback.repository.CustomerFeedbackRepository;
import com.madhurgram.productservice.order.entity.Order;
import com.madhurgram.productservice.order.entity.OrderItem;
import com.madhurgram.productservice.order.repository.OrderRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * Controller for managing buyer testimonials, ratings, and feedback uploads.
 */
@Slf4j
@RestController
@RequestMapping("/api")
@Tag(name = "Customer Feedback", description = "Endpoints for submitting and retrieving reviews, testimonials, and feedback images")
public class CustomerFeedbackController {

    private final CustomerFeedbackRepository feedbackRepository;
    private final OrderRepository orderRepository;

    /**
     * Constructor injection for CustomerFeedbackController.
     *
     * @param feedbackRepository feedback repository
     * @param orderRepository    order repository
     */
    public CustomerFeedbackController(
            CustomerFeedbackRepository feedbackRepository,
            OrderRepository orderRepository) {
        this.feedbackRepository = feedbackRepository;
        this.orderRepository = orderRepository;
    }

    /**
     * Submits customer feedback.
     *
     * @param feedback feedback details payload
     * @return the saved feedback
     */
    @PostMapping("/public/feedback")
    @Operation(summary = "Submit new feedback", description = "Allows a customer to submit their rating, comments, and order references.")
    public ResponseEntity<CustomerFeedback> submitFeedback(@RequestBody CustomerFeedback feedback) {
        log.info("Feedback submission request: Rating={}, Order ID={}", feedback.getRating(), feedback.getOrderId());
        CustomerFeedback saved = feedbackRepository.save(feedback);
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
                        String name = item.getProductName().toLowerCase();
                        if (name.contains("ghee")) hasGhee = true;
                        if (name.contains("oil") || name.contains("tel")) hasOil = true;
                        if (name.contains("honey") || name.contains("shahad")) hasHoney = true;
                        if (name.contains("sweet") || name.contains("mithai") || name.contains("peda") || name.contains("laddu")) hasSweets = true;
                    }
                }

                if (hasGhee) {
                    suggestions.add("Desi Ghee ka swad sach mein lajawab aur shuddh hai! 💛");
                    suggestions.add("Ghee ki dhoop jaisi khushboo ne dil jeet liya.");
                }
                if (hasOil) {
                    suggestions.add("Kachchi ghani tel ki shuddhata 100% genuine hai.");
                    suggestions.add("Tel ki packaging leak-proof aur secure thi.");
                }
                if (hasHoney) {
                    suggestions.add("Pure honey ki mithas aur swad lajawab hai! 🍯");
                }
                if (hasSweets) {
                    suggestions.add("Mithai ka swad bilkul shuddh desi ghee jaisa hai!");
                    suggestions.add("Mithai bohot tazi aur naram thi.");
                }
            }
        }

        // Add default/generic suggestions if list is small to ensure premium options
        if (suggestions.size() < 4) {
            suggestions.add("Delivery bilkul sahi samay par hui. 🚚");
            suggestions.add("MadhurGram ke products ka swad bilkul gaanv jaisa authentic hai! ✨");
            suggestions.add("Packaging bohot surakshit aur clean thi.");
            suggestions.add("Customer support aur ordering experience bohot smooth tha.");
        }

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
            return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
        }
        try {
            String originalFileName = file.getOriginalFilename();
            String extension = "";
            if (originalFileName != null && originalFileName.contains(".")) {
                extension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }
            String fileName = UUID.randomUUID().toString() + extension;

            File uploadDir = new File("uploads/feedback");
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            File destination = new File(uploadDir.getAbsolutePath(), fileName);
            Files.copy(
                file.getInputStream(), 
                destination.toPath(), 
                StandardCopyOption.REPLACE_EXISTING
            );

            // TODO [PRODUCTION]: Replace localhost URL with production static asset CDN
            String fileUrl = "http://localhost:8080/uploads/feedback/" + fileName;
            log.info("Feedback image successfully uploaded: '{}'", fileUrl);
            return ResponseEntity.ok(Map.of("url", fileUrl));
        } catch (Exception e) {
            log.error("Failed to upload feedback image", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to upload file"));
        }
    }

    /**
     * Retrieves positive testimonials to showcase on public homepages.
     *
     * @return top positive feedbacks (rating >= 4)
     */
    @GetMapping("/public/feedback/testimonials")
    @Operation(summary = "Get storefront testimonials", description = "Retrieves the top 8 recent customer feedbacks with 4+ star ratings.")
    public ResponseEntity<List<CustomerFeedback>> getTestimonials() {
        log.info("Request: fetch public testimonials");
        List<CustomerFeedback> testimonials = feedbackRepository.findTop8ByRatingGreaterThanEqualOrderByCreatedAtDesc(4);
        log.info("Returning {} testimonials", testimonials.size());
        return ResponseEntity.ok(testimonials);
    }

    /**
     * Retrieves all customer feedback submissions for the admin dashboard.
     *
     * @return list of feedbacks
     */
    @GetMapping("/admin/feedback")
    @Operation(summary = "List all feedbacks (Admin)", description = "Retrieves all feedback submissions sorted by creation date descending.")
    public ResponseEntity<List<CustomerFeedback>> getFeedbacks() {
        log.info("Admin request: list all feedbacks");
        List<CustomerFeedback> feedbacks = feedbackRepository.findAllByOrderByCreatedAtDesc();
        log.info("Returning {} feedbacks to admin", feedbacks.size());
        return ResponseEntity.ok(feedbacks);
    }
}
