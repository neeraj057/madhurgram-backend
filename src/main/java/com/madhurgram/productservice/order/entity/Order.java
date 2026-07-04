package com.madhurgram.productservice.order.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders", indexes = {
    @Index(name = "idx_orders_phone_number", columnList = "phone_number"),
    @Index(name = "idx_orders_order_date", columnList = "order_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "customer_name")
    private String customerName;

    @Column(nullable = false, name = "phone_number", length = 15)
    private String phoneNumber;

    @Column(nullable = false, length = 500)
    private String address;

    @Column(nullable = false, length = 10)
    private String pincode;

    @Column(nullable = false)
    private String cityState;

    @Column(name = "campaign_id")
    private Long campaignId;

    @Column(nullable = false, name = "total_amount")
    private BigDecimal totalAmount;

    // 🛡️ क्लीन कोड: स्ट्रिंग की जगह Enum मैपिंग और डिफॉल्ट वैल्यू सेट की भाई
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "order_status")
    @Builder.Default
    private OrderStatus orderStatus = OrderStatus.PENDING;

    @Column(name = "order_date")
    private LocalDateTime orderDate;

    // 🔗 @OneToMany Relationship mapping with OrderItem
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference // Parent-Child JSON handling के लिए भाई
    @Builder.Default
    private List<OrderItem> orderItems = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.orderDate = LocalDateTime.now();
    }

    // Helper method: रिलेशनशिप को दोनों तरफ सिंक रखने के लिए
    public void addOrderItem(OrderItem item) {
        orderItems.add(item);
        item.setOrder(this);
    }
}