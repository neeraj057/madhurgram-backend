# 🛡️ MadhurGram Security, Privacy & Compliance Blueprint

This blueprint outlines the technical implementations, code structures, legal drafts, and testing procedures for Data Privacy, Cookie Consents, Data Masking, HTTP Security Headers, Role-Based Access Controls (RBAC), and Audit Logging.

---

## 🗂️ Table of Contents
1. [Data Privacy Framework (Legal Draft Templates)](#1-data-privacy-framework-legal-draft-templates)
2. [Cookie Consent Banner (Frontend Implementation)](#2-cookie-consent-banner-frontend-implementation)
3. [API Security & Data Masking (Technical Shield)](#3-api-security--data-masking-the-technical-shield)
4. [Database Access Control & Audit Logs](#4-database-access-control--audit-logs)
5. [Future Customization & Verification Guides](#5-future-customization--verification-guides)

---

## 1. Data Privacy Framework (Legal Draft Templates)

These policy drafts are customized for MadhurGram's traditional village delivery ecosystem.

### A. Privacy Policy
**Last Updated**: July 2026

At MadhurGram, we respect your privacy and are committed to protecting the personal data of our customers who enjoy Gopiganj's traditional Bilona products.

1. **Information We Collect**
   * **Personal Identifiers**: Name, shipping address, billing address, and phone number (used for delivery routing and status notifications).
   * **Payment Information**: Processed securely via authorized payment gateways (Razorpay/UPI). MadhurGram *never* stores raw credit/debit card numbers or net banking credentials.
2. **How We Use Your Information**
   * To fulfill and deliver orders of ghee, honey, oil, and sweets.
   * To send automated delivery status tracking alerts via Twilio SMS/WhatsApp.
   * To gather optional customer feedback to improve food packaging and purity.
3. **Data Retention Policy**
   * Customer profiles and order history are retained in our secure database for accounting, audit, and tax compliances.
   * Inactive carts are automatically purged after **48 hours** via scheduled background database cleansers.
4. **Your Data Rights**
   * Customers can request to view, edit, or purge their personal delivery address databases by contacting `support@madhurgram.com`.

---

### B. Terms & Conditions
**Last Updated**: July 2026

Welcome to MadhurGram. By accessing this store, you agree to comply with the terms and conditions outlined below.

1. **Ordering & Freshness Heritage**
   * All MadhurGram products (including hand-churned Bilona Cow Ghee) are handcrafted in small batches. Variations in grain size, texture, and aroma are natural characteristics of traditional processing.
2. **Return & Refund Policy**
   * Since our products are consumable food items, we **do not accept returns** once the package seal has been opened or broken.
   * In case of delivery damage (e.g. broken bottles or packages), customers must share a photo of the damaged package with customer support within **24 hours** of delivery to claim a replacement or refund.
3. **Delivery Terms & Courier Liability**
   * We ship orders using third-party logistics. While we make every attempt to deliver within standard timelines (3-5 business days), MadhurGram is **not liable** for delivery delays caused by logistics partners, courier strikes, or adverse weather conditions.
4. **Limitation of Liability**
   * MadhurGram's total liability for any claim regarding order delivery is strictly capped at the invoice value of the corresponding transaction.

---

### C. Cancellation & Refund Policy
**Last Updated**: July 2026

1. **Order Cancellation**
   * Customers can cancel an order within **2 hours** of placement or before the status transitions to **Confirmed** / **Shipped** (whichever is earlier). Once shipped, cancellation requests cannot be accepted.
   * To request cancellation, contact customer helpline at `support@madhurgram.com` or call `+91 98765 43210`.
2. **Refund Process**
   * Approved refunds for cancellations or damaged goods are processed within **5-7 business days**.
   * Refunds will be credited directly to the original payment source (bank account, credit card, or UPI wallet) used during transaction checkout.

---

## 2. Cookie Consent Banner (Frontend Implementation)

Below is the complete React implementation of a premium, non-intrusive Cookie Consent banner using Tailwind CSS.

### A. Component Code: `src/components/common/CookieConsent.tsx`
```tsx
"use client";

import React, { useState, useEffect } from "react";
import { Shield, Check, X } from "lucide-react";

export default function CookieConsent() {
  const [showBanner, setShowBanner] = useState(false);

  useEffect(() => {
    // Check if consent has already been accepted
    const consent = localStorage.getItem("mg_cookie_consent");
    if (!consent) {
      const timer = setTimeout(() => setShowBanner(true), 1500); // delay load
      return () => clearTimeout(timer);
    }
  }, []);

  const handleAccept = () => {
    localStorage.setItem("mg_cookie_consent", "accepted");
    setShowBanner(false);
  };

  const handleDecline = () => {
    localStorage.setItem("mg_cookie_consent", "declined");
    setShowBanner(false);
  };

  if (!showBanner) return null;

  return (
    <div className="fixed bottom-6 right-6 z-50 max-w-sm w-full animate-slide-up">
      <div className="bg-[#121212] border border-gray-800 rounded-2xl p-5 shadow-2xl backdrop-blur-md relative overflow-hidden">
        {/* Subtle gold glow card element */}
        <div className="absolute top-0 left-0 w-full h-[2px] bg-gradient-to-r from-transparent via-[#D4AF37] to-transparent" />
        
        <div className="flex items-start gap-4">
          <div className="h-10 w-10 shrink-0 rounded-full bg-[#D4AF37]/10 border border-[#D4AF37]/20 flex items-center justify-center text-[#D4AF37]">
            <Shield className="h-5 w-5" />
          </div>

          <div className="space-y-3">
            <h4 className="text-xs font-bold uppercase tracking-wider text-[#FDFBF7]">Cookie Consent</h4>
            <p className="text-[11px] text-gray-400 leading-relaxed font-light">
              MadhurGram uses cookies to enhance your shopping experience and optimize our local Bilona product delivery logs. Do you agree?
            </p>
            
            <div className="flex gap-2 pt-2 border-t border-gray-800/40">
              <button
                onClick={handleAccept}
                className="flex-1 py-2 px-3 rounded-lg bg-gradient-to-r from-[#D4AF37] to-[#F3E5AB] text-[10px] font-bold uppercase tracking-widest text-[#111111] hover:brightness-110 active:scale-95 transition-all text-center"
              >
                Accept
              </button>
              <button
                onClick={handleDecline}
                className="flex-1 py-2 px-3 rounded-lg bg-[#181818] border border-gray-800 text-[10px] font-bold uppercase tracking-widest text-gray-400 hover:text-white hover:border-gray-700 active:scale-95 transition-all text-center"
              >
                Decline
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
```

### B. Integration into `src/app/layout.tsx`
Wrap or place the component inside the root layout:
```tsx
import CookieConsent from "@/components/common/CookieConsent";

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <body>
        <MaintenanceProvider>
          {children}
        </MaintenanceProvider>
        <CookieConsent />
      </body>
    </html>
  );
}
```

---

## 3. API Security & Data Masking (The Technical Shield)

### A. Backend Spring Security HTTP Security Headers
Override filters inside `SecurityConfig.java` to set headers preventing clickjacking, MIME-sniffing, and XSS cross-site script injections.

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        // CORS & CSRF configurations
        .csrf(csrf -> csrf.disable())
        .headers(headers -> headers
            // Enable HTTP strict transport security (HSTS)
            .httpStrictTransportSecurity(hsts -> hsts
                .includeSubDomains(true)
                .maxAgeInSeconds(31536000)
            )
            // X-Frame-Options: Prevent Clickjacking
            .frameOptions(frame -> frame.deny())
            // X-Content-Type-Options: Prevent MIME sniffing
            .contentTypeOptions(Customizer.withDefaults())
            // X-XSS-Protection: Cross-Site Scripting filter
            .xssProtection(xss -> xss.headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
        );
    return http.build();
}
```

### B. Java Utility Class for Data Masking
Implement this helper class on the backend to obfuscate sensitive customer fields before transmitting payloads to non-superadmin users.

```java
package com.madhurgram.productservice.common.util;

public class DataMaskingUtil {

    /**
     * Obfuscates phone numbers to +91-XXXXX-9988 format
     */
    public static String maskPhoneNumber(String phone) {
        if (phone == null || phone.trim().length() < 10) {
            return phone;
        }
        String cleanPhone = phone.trim();
        int len = cleanPhone.length();
        
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
```

---

## 4. Database Access Control & Audit Logs

### A. Role-Based Access Control (RBAC) Schema
In the administrative console database, set roles to determine permissions:
1. `ROLE_SUPER_ADMIN`: Access to modify prices, update payment gates, delete orders, and access raw customer data logs.
2. `ROLE_SUPPORT_STAFF`: Read-only access to products and orders. Access to mask phone logs. Restrictive access preventing price changes.

#### Spring Security Route Protection
```java
http.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/admin/settings/**").hasRole("SUPER_ADMIN")
    .requestMatchers("/api/admin/products/delete/**").hasRole("SUPER_ADMIN")
    .requestMatchers("/api/admin/products/price").hasRole("SUPER_ADMIN")
    .requestMatchers("/api/admin/orders/**").hasAnyRole("SUPER_ADMIN", "SUPPORT_STAFF")
    .anyRequest().authenticated()
);
```

### B. Audit Log Tracker Setup
A custom entity and interceptor to log sensitive price modifications (Pricing Audits).

#### 1. Audit Log Entity
```java
package com.madhurgram.productservice.audit.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "action_type", nullable = false)
    private String actionType; // e.g. "PRICE_CHANGE", "SETTINGS_UPDATE"

    @Column(name = "modified_by", nullable = false)
    private String modifiedBy; // Username / email

    @Column(name = "details", length = 2000)
    private String details; // e.g. "Product Ghee price modified from 1200 to 1400"

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        this.timestamp = LocalDateTime.now();
    }
    
    // Getters, Setters, Constructors...
}
```

#### 2. Price Update Interceptor Logger (Java Controller implementation)
```java
@PostMapping("/admin/products/{id}/price")
public ResponseEntity<?> updateProductPrice(
        @PathVariable Long id, 
        @RequestParam Double newPrice, 
        @AuthenticationPrincipal UserDetails userDetails) {
        
    Product product = productService.getProductById(id);
    Double oldPrice = product.getPrice();
    product.setPrice(newPrice);
    productService.save(product);

    // Save to Audit Log
    AuditLog log = new AuditLog();
    log.setActionType("PRICE_CHANGE");
    log.setModifiedBy(userDetails.getUsername());
    log.setDetails(String.format("Product '%s' (ID: %d) price changed from %.2f to %.2f", 
                  product.getName(), id, oldPrice, newPrice));
                  
    auditLogRepository.save(log);
    return ResponseEntity.ok().build();
}
```

---

## 5. Future Customization & Verification Guides

### A. How to Edit Policy Drafts
1. **Frontend Legal Routes**:
   * Add text files or routing files at `/privacy-policy`, `/terms`, and `/refunds` using the drafted markdown templates.
2. **Editing Legal Text**:
   * Modify the templates directly in your editor. Ensure you update the **Last Updated** header timestamp.

### B. How to Test Cookie Consent manually
1. Launch the storefront website at `http://localhost:3000/`.
2. Wait 1.5 seconds. Check if the bottom-right Cookie Banner pops up.
3. Open browser developer console (F12) ➔ **Application** ➔ **Local Storage** ➔ `http://localhost:3000`.
4. Click **Accept** on the popup. Verify that `mg_cookie_consent: accepted` is set and the banner closes.
5. Clear Local Storage and refresh to re-test the **Decline** button.

### C. How to Test Data Masking
1. Log in to admin console using a **Support Staff** credential.
2. Request `/api/admin/orders`. Inspect HTTP response in Network tab.
3. Confirm that the phone number fields return values obfuscated in `+91-XXXXX-9988` format.

### D. How to Test Role Controls (RBAC) & Audit Logs
1. Attempt to send a `POST /api/admin/settings` request using a **Support Staff** API Token.
2. Confirm the server returns HTTP status `403 Forbidden`.
3. Perform a price update using a **Super Admin** token.
4. Query the `audit_logs` table in MySQL:
   `SELECT * FROM audit_logs ORDER BY timestamp DESC;`
5. Verify that a record containing the action `PRICE_CHANGE`, the admin's username, and details of the price change has been recorded.
