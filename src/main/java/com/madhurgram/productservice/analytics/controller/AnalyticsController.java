package com.madhurgram.productservice.analytics.controller;

import com.madhurgram.productservice.analytics.dto.AdminAnalyticsDTO;
import com.madhurgram.productservice.analytics.service.AnalyticsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for retrieving dashboard analytics and reporting data for
 * administrators.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/analytics")
@Tag(name = "Admin — Analytics", description = "Endpoints for administrator business reports and charts analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    /**
     * Constructor injection for AnalyticsController.
     *
     * @param analyticsService analytics calculations service
     */
    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    /**
     * Computes daily business metrics report.
     *
     * @param days cutoff time range in number of days (defaults to 7)
     * @return daily dashboard metrics details DTO
     */
    @GetMapping("/daily")
    @Operation(summary = "Get daily business report", description = "Retrieves sales, orders, and user analytics aggregation for dashboard widgets.")
    public ResponseEntity<AdminAnalyticsDTO> getDailyReport(@RequestParam(defaultValue = "7") int days) {
        log.info("Admin request: fetch daily business report for range: {} day(s)", days);
        AdminAnalyticsDTO metrics = analyticsService.getDailyDashboardMetrics(days);
        log.info("Successfully calculated and retrieved dashboard metrics");
        return ResponseEntity.ok(metrics);
    }
}