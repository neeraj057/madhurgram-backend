package com.madhurgram.productservice.analytics.service;

import com.madhurgram.productservice.analytics.dto.AdminAnalyticsDTO;

public interface AnalyticsService {
    AdminAnalyticsDTO getDailyDashboardMetrics(int days);
}