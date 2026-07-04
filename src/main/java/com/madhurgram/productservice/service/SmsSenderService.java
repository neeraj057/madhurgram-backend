package com.madhurgram.productservice.service;

import java.util.List;

public interface SmsSenderService {
    int sendBroadcastMessage(List<String> recipients, String message);
}
