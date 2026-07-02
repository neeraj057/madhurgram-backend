package com.madhurgram.productservice.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "addresses")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AddressType addressType; // HOME, OFFICE, OTHER

    @Column(nullable = false, length = 255)
    private String fullAddress;

    @Column(nullable = false, length = 50)
    private String city;

    @Column(nullable = false, length = 50)
    private String state;

    @Column(nullable = false, length = 10)
    private String pincode;

    @Column(nullable = false)
    private Boolean isDefault = false; // बाय डिफ़ॉल्ट कौन सा एड्रेस सिलेक्टेड रहेगा

    // 🔗 Many-to-One Mapping: यह एड्रेस किस कस्टमर का है?
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    @JsonIgnore // ताकि API रिस्पॉन्स में इनफाइनाइट लूप न बने
    @ToString.Exclude
    private Customer customer;
}