package com.madhurgram.productservice.customer.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "customers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 📞 फोन नंबर ही हमारा प्राइमरी आइडेंटिटी (Unique Key) होगा
    @Column(nullable = false, unique = true, length = 15)
    private String phoneNumber;

    @Column(length = 100)
    private String fullName;

    @Column(length = 100)
    private String email;

    // 🔗 One-to-Many Mapping: एक कस्टमर के पास कई एड्रेस हो सकते हैं
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Address> addresses = new ArrayList<>();

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // Helper method to keep bi-directional relationship in sync
    public void addAddress(Address address) {
        addresses.add(address);
        address.setCustomer(this);
    }
}