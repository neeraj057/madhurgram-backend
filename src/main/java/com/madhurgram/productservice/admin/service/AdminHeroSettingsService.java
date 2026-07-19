package com.madhurgram.productservice.admin.service;

import java.util.Map;

public interface AdminHeroSettingsService {
    Map<String, String> getHeroSettings();
    Map<String, String> updateHeroSettings(Map<String, String> payload);
}
