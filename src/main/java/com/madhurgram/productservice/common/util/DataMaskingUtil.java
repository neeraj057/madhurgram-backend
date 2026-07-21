package com.madhurgram.productservice.common.util;

/**
 * Utility class for obfuscating sensitive customer data (such as phone numbers 
 * and email addresses) for security and privacy protection.
 */
public final class DataMaskingUtil {

    private static final String MASK_PHONE_PREFIX = "+91-XXXXX-";
    private static final String SHORT_PHONE_MASK = "****";
    private static final String EMPTY_STRING = "";
    
    private static final String EMAIL_AT_SYMBOL = "@";
    private static final String EMAIL_MASK_MIDDLE = "***";

    private DataMaskingUtil() {
        // Prevent instantiation of utility class
        throw new UnsupportedOperationException("Utility class should not be instantiated.");
    }

    /**
     * Obfuscates phone numbers to +91-XXXXX-9988 format.
     *
     * @param phone the raw phone number to mask
     * @return the masked phone number string
     */
    public static String maskPhoneNumber(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return EMPTY_STRING;
        }
        
        String cleanPhone = phone.trim();
        int len = cleanPhone.length();
        
        if (len < 4) {
            return SHORT_PHONE_MASK;
        }
        
        // Retain last 4 digits, replace middle block with XXXXX
        String lastFour = cleanPhone.substring(len - 4);
        return MASK_PHONE_PREFIX + lastFour;
    }

    /**
     * Obfuscates email addresses to a***b@domain.com format.
     *
     * @param email the raw email address to mask
     * @return the masked email string
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains(EMAIL_AT_SYMBOL)) {
            return email;
        }
        String cleanEmail = email.trim();
        int atIndex = cleanEmail.indexOf(EMAIL_AT_SYMBOL);
        if (atIndex <= 2) {
            return EMAIL_MASK_MIDDLE + cleanEmail.substring(atIndex);
        }
        
        String name = cleanEmail.substring(0, atIndex);
        String domain = cleanEmail.substring(atIndex);
        
        return name.charAt(0) + EMAIL_MASK_MIDDLE + name.charAt(name.length() - 1) + domain;
    }
    
    /**
     * Obfuscates names to J***e format.
     *
     * @param name the raw name to mask
     * @return the masked name string
     */
    public static String maskName(String name) {
        if (name == null || name.trim().length() < 3) {
            return name;
        }
        String cleanName = name.trim();
        return cleanName.charAt(0) + "***" + cleanName.charAt(cleanName.length() - 1);
    }

    /**
     * Obfuscates IPv4 addresses.
     *
     * @param ip the raw IP address to mask
     * @return the masked IP address string
     */
    public static String maskIpAddress(String ip) {
        if (ip == null || ip.isEmpty()) return ip;
        // Basic masking for IPv4
        return ip.replaceAll("\\.\\d+\\.\\d+$", ".***.***");
    }
}
