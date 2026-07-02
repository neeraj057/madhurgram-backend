package com.madhurgram.productservice.controller;

import com.madhurgram.productservice.dto.AdminAnalyticsDTO;
import com.madhurgram.productservice.service.AnalyticsService;
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
    public ResponseEntity<AdminAnalyticsDTO> getDailyReport() {
        AdminAnalyticsDTO metrics = analyticsService.getDailyDashboardMetrics();
        return ResponseEntity.ok(metrics);
    }
}