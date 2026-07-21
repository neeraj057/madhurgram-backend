package com.madhurgram.productservice.customer.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Customer
 */
@Entity
@Table(name = "customers", indexes = {
        @Index(name = "idx_customer_phone", columnList = "phoneNumber", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 📞 phone number is the primary key
    @Column(nullable = false, unique = true, length = 15)
    private String phoneNumber;

    @Column(length = 100)
    private String fullName;

    @Column(length = 100)
    private String email;

    // 🔗 One-to-Many Mapping: a customer can have multiple addresses
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