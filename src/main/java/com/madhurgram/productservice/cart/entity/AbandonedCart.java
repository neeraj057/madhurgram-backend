package com.madhurgram.productservice.cart.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "abandoned_carts", indexes = {
        @Index(name = "idx_abandoned_carts_phone", columnList = "phone_number"),
        @Index(name = "idx_abandoned_carts_recovered_updated", columnList = "is_recovered, last_updated")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AbandonedCart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "phone_number", nullable = false, length = 15)
    private String phoneNumber;

    @Column(name = "customer_name", length = 100)
    private String customerName;

    @Lob
    @Column(name = "cart_items_json", nullable = false, columnDefinition = "TEXT")
    private String cartItemsJson;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;

    @Column(name = "is_recovered", nullable = false)
    @Builder.Default
    private boolean isRecovered = false;

    @Column(name = "reminder_sent", nullable = false)
    @Builder.Default
    private boolean reminderSent = false;

    @Column(name = "reminder_sent_at")
    private LocalDateTime reminderSentAt;

}
