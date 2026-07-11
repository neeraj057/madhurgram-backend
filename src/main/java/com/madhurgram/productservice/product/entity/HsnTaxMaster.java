package com.madhurgram.productservice.product.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "hsn_tax_master")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HsnTaxMaster {

    @Id
    @Column(name = "hsn_code", length = 10)
    private String hsnCode; // e.g., "0405", "1701"

    @Column(nullable = false, length = 150)
    private String description;

    @Column(name = "gst_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal gstRate; // e.g., 12.00, 5.00
}
