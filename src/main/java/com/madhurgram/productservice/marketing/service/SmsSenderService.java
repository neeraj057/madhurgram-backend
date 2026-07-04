package com.madhurgram.productservice.marketing.service;

import java.util.List;

public interface SmsSenderService {
    int sendBroadcastMessage(List<String> recipients, String message);
    void sendBroadcastMessageAsync(Long campaignId, List<String> recipients, String message);
}
