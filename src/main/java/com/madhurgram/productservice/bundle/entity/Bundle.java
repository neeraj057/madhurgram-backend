package com.madhurgram.productservice.bundle.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an admin-created product bundle (combo offer).
 * Admin controls name, discount, active status, and which products are included.
 * The storefront fetches only active bundles via the public API.
 */
@Entity
@Table(name = "bundles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bundle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Short name shown on tab e.g. "Rasoi Combo" */
    @Column(name = "tab_name", nullable = false, length = 100)
    private String tabName;

    /** Full display name e.g. "Complete Kitchen Combo (Ghee + Oil + Pickle)" */
    @Column(nullable = false, length = 255)
    private String name;

    /** Storefront description — admin writes this */
    @Column(columnDefinition = "TEXT")
    private String description;

    /** Discount % that admin decides — e.g. 15 means 15% off sum of item prices */
    @Column(name = "discount_percent", nullable = false)
    @Builder.Default
    private Integer discountPercent = 10;

    /** Admin toggles this to show/hide on storefront */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = false;

    /** Controls display order in storefront tabs */
    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** The products that make up this bundle */
    @OneToMany(mappedBy = "bundle", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<BundleItem> items = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
