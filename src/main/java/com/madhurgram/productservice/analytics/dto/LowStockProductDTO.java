package com.madhurgram.productservice.analytics.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LowStockProductDTO {
    private Long id;
    private String name;
    private Integer stock;
    private BigDecimal price;
}
