package com.madhurgram.productservice.common.service.impl;

import com.madhurgram.productservice.common.entity.SystemSetting;
import com.madhurgram.productservice.common.repository.SystemSettingRepository;
import com.madhurgram.productservice.common.service.SystemSettingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for dynamic configuration management.
 */
@Slf4j
@Service
public class SystemSettingServiceImpl implements SystemSettingService {

    private final SystemSettingRepository repository;

    public SystemSettingServiceImpl(SystemSettingRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public String getSettingValue(String key, String defaultValue) {
        return repository.findById(key)
                .map(SystemSetting::getSettingValue)
                .orElse(defaultValue);
    }

    @Override
    @Transactional
    public void saveSetting(String key, String value, String description) {
        SystemSetting setting = repository.findById(key)
                .orElse(SystemSetting.builder()
                        .settingKey(key)
                        .description(description)
                        .build());
        setting.setSettingValue(value);
        repository.save(setting);
        log.info("System setting '{}' updated to '{}'", key, value);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean getSettingBoolean(String key, boolean defaultValue) {
        String value = getSettingValue(key, String.valueOf(defaultValue));
        return "true".equalsIgnoreCase(value);
    }
}
