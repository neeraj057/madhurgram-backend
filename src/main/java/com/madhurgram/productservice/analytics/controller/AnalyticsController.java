package com.madhurgram.productservice.analytics.controller;

import com.madhurgram.productservice.analytics.dto.AdminAnalyticsDTO;
import com.madhurgram.productservice.analytics.service.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/analytics")
@CrossOrigin(origins = "*") // CORS guard sync
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/daily")
    public ResponseEntity<AdminAnalyticsDTO> getDailyReport(@RequestParam(defaultValue = "7") int days) {
        AdminAnalyticsDTO metrics = analyticsService.getDailyDashboardMetrics(days);
        return ResponseEntity.ok(metrics);
    }
}