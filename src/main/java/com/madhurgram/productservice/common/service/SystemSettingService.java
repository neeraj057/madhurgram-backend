package com.madhurgram.productservice.common.service;

/**
 * Service interface for managing dynamic configurations in the system_settings table.
 */
public interface SystemSettingService {

    /**
     * Retrieves a setting value by key, returning a default value if not found.
     *
     * @param key          the setting key to query
     * @param defaultValue the fallback value if the key does not exist
     * @return the active setting value or fallback default
     */
    String getSettingValue(String key, String defaultValue);

    /**
     * Saves or updates a setting value.
     *
     * @param key         the setting key
     * @param value       the value to save
     * @param description the description explaining the setting key
     */
    void saveSetting(String key, String value, String description);

    /**
     * Retrieves a setting value and checks if it evaluates to true.
     *
     * @param key          the setting key
     * @param defaultValue the fallback boolean value
     * @return true if the setting is equal to "true"
     */
    boolean getSettingBoolean(String key, boolean defaultValue);
}
