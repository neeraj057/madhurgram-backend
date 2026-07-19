package com.madhurgram.productservice.common.constants;

/**
 * Constants for System Setting database keys.
 */
public final class SystemSettingKeys {

    private SystemSettingKeys() {
        // Prevent instantiation of utility class
        throw new UnsupportedOperationException("Utility class should not be instantiated.");
    }

    public static final String WHATSAPP_QUICK_BUY_ENABLED = "WHATSAPP_QUICK_BUY_ENABLED";
    public static final String WHATSAPP_QUICK_BUY_NUMBER = "WHATSAPP_QUICK_BUY_NUMBER";
    public static final String WHATSAPP_QUICK_BUY_TEXT_TEMPLATE = "WHATSAPP_QUICK_BUY_TEXT_TEMPLATE";

    public static final String PINCODE_SLA_LOCAL = "PINCODE_SLA_LOCAL";
    public static final String PINCODE_SLA_REGIONAL = "PINCODE_SLA_REGIONAL";
    public static final String PINCODE_SLA_NATIONAL = "PINCODE_SLA_NATIONAL";
}
