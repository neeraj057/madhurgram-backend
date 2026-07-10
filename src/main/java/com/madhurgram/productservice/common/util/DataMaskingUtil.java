package com.madhurgram.productservice.common.util;

public class DataMaskingUtil {

    /**
     * Obfuscates phone numbers to +91-XXXXX-9988 format
     */
    public static String maskPhoneNumber(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return "";
        }
        
        String cleanPhone = phone.trim();
        int len = cleanPhone.length();
        
        if (len < 4) {
            return "****";
        }
        
        // Retain last 4 digits, replace middle block with XXXXX
        String lastFour = cleanPhone.substring(len - 4);
        return "+91-XXXXX-" + lastFour;
    }

    /**
     * Obfuscates email addresses to a***b@domain.com format
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        String cleanEmail = email.trim();
        int atIndex = cleanEmail.indexOf("@");
        if (atIndex <= 2) {
            return "***" + cleanEmail.substring(atIndex);
        }
        
        String name = cleanEmail.substring(0, atIndex);
        String domain = cleanEmail.substring(atIndex);
        
        return name.charAt(0) + "***" + name.charAt(name.length() - 1) + domain;
    }
}
