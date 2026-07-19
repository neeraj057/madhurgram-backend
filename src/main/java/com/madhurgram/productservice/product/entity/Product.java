package com.madhurgram.productservice.product.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(name = "original_price", nullable = true)
    private BigDecimal originalPrice;

    @Column(nullable = false, length = 50)
    private String volume; // e.g., "500ml", "1kg"

    @Column(name = "image_url", length = 500)
    private String imageUrl; // Cloud/S3 image link mapped here

    @Column(length = 50)
    private String tag; // e.g., "Bilona Method", "New", "Out of Stock"

    @Column(nullable = false, length = 50)
    private String category; // e.g., "dairy", "sweeteners", "oils", "pickles"

    @Column(nullable = false)
    private Integer stock; // Inventory tracking for "Out of Stock" logic

    @Builder.Default
    @Column(name = "is_active")
    private boolean isActive = true;

    @Builder.Default
    @Column(name = "rating", precision = 3, scale = 2)
    private java.math.BigDecimal rating = java.math.BigDecimal.valueOf(4.8);

    @Builder.Default
    @Column(name = "show_sales_count")
    private Boolean showSalesCount = false;

    @Builder.Default
    @Column(name = "sales_count")
    private Integer salesCount = 0;

    @org.hibernate.annotations.Formula("(SELECT COALESCE(SUM(oi.quantity), 0) FROM order_items oi JOIN orders o ON oi.order_id = o.id WHERE oi.product_id = id AND o.order_status != 'CANCELLED')")
    private Integer realSalesCount;

    @Builder.Default
    @Column(name = "clearance_active")
    private Boolean clearanceActive = false;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "hsn_code", referencedColumnName = "hsn_code", nullable = true)
    private HsnTaxMaster hsnTaxMaster;
}