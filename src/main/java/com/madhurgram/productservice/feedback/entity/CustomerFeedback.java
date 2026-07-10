package com.madhurgram.productservice.feedback.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "customer_feedbacks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "customer_name", nullable = false, length = 150)
    private String customerName;

    @Column(nullable = false, length = 50)
    private String sentiment; // e.g., "LOVED_IT", "HAPPY", "NEUTRAL", "SAD", "ANGRY"

    @Column(nullable = false)
    private Integer rating; // 1 to 5

    @Column(name = "feedback_text", length = 1000)
    private String feedbackText;

    @Column(name = "selected_chips", length = 1000)
    private String selectedChips; // Comma-separated selected suggestions

    @Column(name = "product_image_url", length = 1000)
    private String productImageUrl; // Customer-uploaded product image URL

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
