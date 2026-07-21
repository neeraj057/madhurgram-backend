package com.madhurgram.productservice.payment.entity;

/**
 * Enumeration of supported payment gateway providers and checkout options.
 */
public enum PaymentProvider {
    RAZORPAY,
    STRIPE,
    COD;

    /**
     * Case-insensitive lookup for payment provider.
     *
     * @param provider string representation of provider
     * @return matching PaymentProvider, or COD default if null/unrecognized
     */
    public static PaymentProvider fromString(String provider) {
        if (provider == null || provider.isBlank()) {
            return COD;
        }
        try {
            return PaymentProvider.valueOf(provider.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return COD;
        }
    }
}
