package com.madhurgram.productservice.feedback.controller;

import com.madhurgram.productservice.feedback.entity.CustomerFeedback;
import com.madhurgram.productservice.feedback.repository.CustomerFeedbackRepository;
import com.madhurgram.productservice.order.entity.Order;
import com.madhurgram.productservice.order.entity.OrderItem;
import com.madhurgram.productservice.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class CustomerFeedbackController {

    private static final Logger log = LoggerFactory.getLogger(CustomerFeedbackController.class);

    private final CustomerFeedbackRepository feedbackRepository;
    private final OrderRepository orderRepository;

    public CustomerFeedbackController(
            CustomerFeedbackRepository feedbackRepository,
            OrderRepository orderRepository) {
        this.feedbackRepository = feedbackRepository;
        this.orderRepository = orderRepository;
    }

    @PostMapping("/public/feedback")
    public ResponseEntity<CustomerFeedback> submitFeedback(@RequestBody CustomerFeedback feedback) {
        log.info("[FEEDBACK] Submitting new feedback. Rating: {}, Customer: {}, Order ID: {}", 
                feedback.getRating(), feedback.getCustomerName(), feedback.getOrderId());
        CustomerFeedback saved = feedbackRepository.save(feedback);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/public/feedback/suggestions")
    public ResponseEntity<List<String>> getFeedbackSuggestions(@RequestParam(required = false) Long orderId) {
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

    @PostMapping("/public/feedback/upload")
    public ResponseEntity<java.util.Map<String, String>> uploadFeedbackImage(@RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", "File is empty"));
        }
        try {
            String originalFileName = file.getOriginalFilename();
            String extension = "";
            if (originalFileName != null && originalFileName.contains(".")) {
                extension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }
            String fileName = java.util.UUID.randomUUID().toString() + extension;

            java.io.File uploadDir = new java.io.File("uploads/feedback");
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            java.io.File destination = new java.io.File(uploadDir.getAbsolutePath(), fileName);
            java.nio.file.Files.copy(
                file.getInputStream(), 
                destination.toPath(), 
                java.nio.file.StandardCopyOption.REPLACE_EXISTING
            );

            String fileUrl = "http://localhost:8080/uploads/feedback/" + fileName;
            return ResponseEntity.ok(java.util.Map.of("url", fileUrl));
        } catch (Exception e) {
            log.error("[FEEDBACK] Failed to upload feedback image", e);
            return ResponseEntity.status(500).body(java.util.Map.of("error", "Failed to upload file"));
        }
    }

    @GetMapping("/public/feedback/testimonials")
    public ResponseEntity<List<CustomerFeedback>> getTestimonials() {
        log.info("[FEEDBACK] Fetching public positive testimonials");
        List<CustomerFeedback> testimonials = feedbackRepository.findTop8ByRatingGreaterThanEqualOrderByCreatedAtDesc(4);
        return ResponseEntity.ok(testimonials);
    }

    @GetMapping("/admin/feedback")
    public ResponseEntity<List<CustomerFeedback>> getFeedbacks() {
        log.info("[FEEDBACK] Fetching all customer feedbacks for administrative dashboard");
        List<CustomerFeedback> feedbacks = feedbackRepository.findAllByOrderByCreatedAtDesc();
        return ResponseEntity.ok(feedbacks);
    }
}
