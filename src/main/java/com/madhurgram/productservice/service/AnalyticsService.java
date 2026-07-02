package com.madhurgram.productservice.service;

import com.madhurgram.productservice.dto.AdminAnalyticsDTO;

public interface AnalyticsService {
    AdminAnalyticsDTO getDailyDashboardMetrics();
}