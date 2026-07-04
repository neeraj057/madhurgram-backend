package com.madhurgram.productservice.analytics.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminAnalyticsDTO {
    private BigDecimal todayRevenue;       // आज की कुल कमाई
    private Long todayOrderCount;          // आज आए कुल ऑर्डर्स
    private Long pendingOrderCount;        // कुल पेंडिंग ऑर्डर्स (जो अभी प्रोसेस होने हैं)
    private Long lowStockProductCount;     // कितने प्रोडक्ट्स का स्टॉक खत्म होने वाला है (< 5)
}