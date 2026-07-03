package com.madhurgram.productservice.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDTO {
    private Long id;
    private String name;
    private BigDecimal price;
    private String volume;
    private String imageUrl;
    private Integer stock;
    private Boolean isActive;
    private String category;
}