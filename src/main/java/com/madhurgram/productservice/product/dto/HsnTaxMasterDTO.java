package com.madhurgram.productservice.product.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HsnTaxMasterDTO {
    private String hsnCode;
    private String description;
    private BigDecimal gstRate;
}
