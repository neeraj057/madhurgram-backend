package com.madhurgram.productservice.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "broadcast_campaigns")
public class BroadcastCampaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(nullable = false, length = 50)
    private String targetSegment;

    @Column(length = 120)
    private String productKeyword;

    @Column(name = "product_id")
    private Long productId;

    @Column(nullable = false)
    private int recipients;

    @Column(nullable = false)
    private int conversions;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public BroadcastCampaign() {}

    public BroadcastCampaign(String title, String message, String targetSegment, String productKeyword, Long productId, int recipients, int conversions, LocalDateTime createdAt) {
        this.title = title;
        this.message = message;
        this.targetSegment = targetSegment;
        this.productKeyword = productKeyword;
        this.productId = productId;
        this.recipients = recipients;
        this.conversions = conversions;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getTargetSegment() {
        return targetSegment;
    }

    public String getProductKeyword() {
        return productKeyword;
    }

    public Long getProductId() {
        return productId;
    }

    public int getRecipients() {
        return recipients;
    }

    public int getConversions() {
        return conversions;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setRecipients(int recipients) {
        this.recipients = recipients;
    }

    public void setConversions(int conversions) {
        this.conversions = conversions;
    }
}
