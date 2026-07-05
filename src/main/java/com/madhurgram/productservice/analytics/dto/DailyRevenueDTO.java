package com.madhurgram.productservice.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyRevenueDTO {
    private String date; // "YYYY-MM-DD" formatted date string
    private BigDecimal revenue;
}
