package com.madhurgram.productservice.returns.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "return_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReturnRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "customer_phone", nullable = false, length = 15)
    private String customerPhone;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(nullable = false, length = 30)
    private String status; // PENDING, APPROVED, REJECTED

    @Column(name = "refund_transaction_id", length = 100)
    private String refundTransactionId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
