package com.madhurgram.productservice.bundle.entity;

import com.madhurgram.productservice.product.entity.Product;
import jakarta.persistence.*;
import lombok.*;

/**
 * Join entity between a Bundle and a Product.
 * One bundle can have multiple products; each row is one product in a bundle.
 */
@Entity
@Table(name = "bundle_items", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"bundle_id", "product_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BundleItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bundle_id", nullable = false)
    private Bundle bundle;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
}
