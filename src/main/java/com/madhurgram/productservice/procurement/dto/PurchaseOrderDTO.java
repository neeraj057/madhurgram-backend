package com.madhurgram.productservice.procurement.dto;

import com.madhurgram.productservice.product.dto.ProductDTO;
import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderDTO {
    private Long id;
    private ProductDTO product;
    private Integer quantity;
    private String supplierName;
    private String supplierEmail;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime approvedAt;
}
