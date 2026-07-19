# Pincode Delivery SLA & Courier Availability Checker Architecture

This document details the design and flow of **Feature 2: Pincode Delivery SLA & Courier Availability Checker** implemented across the MadhurGram platform.

---

## 📍 1. Feature Description

Ensuring that customers know exactly when they will receive their pure, traditional goods is key to building trust. 

The **Pincode Delivery SLA Checker** enables storefront customers to input their 6-digit postal code (PIN code) directly on the product quick buy view and verify:
1. If MadhurGram delivers to their location.
2. The expected delivery duration (SLA).
3. The logistic dispatch mechanism (e.g., Shiprocket Express).

---

## 🗄️ 2. Database & SLA Tier Mapping (Backend)

The backend implements a three-tier geographic routing model.

### A. Database Configurations
The expected delivery durations are saved in the `system_settings` table:
* `PINCODE_SLA_LOCAL`: Timeline for Varanasi and Bhadohi districts (default: `1-2 Business Days`).
* `PINCODE_SLA_REGIONAL`: Timeline for rest of Uttar Pradesh state (default: `2-3 Business Days`).
* `PINCODE_SLA_NATIONAL`: Timeline for rest of India states (default: `4-6 Business Days`).

### B. SLA Resolver Endpoint (`PincodeSlaController.java`)
Exposes `GET /api/public/pincode/check?pincode={pincode}`.

#### Step 1: External Postal API Fetch (Primary)
The backend calls the public Indian Postal Directory API (`https://api.postalpincode.in/pincode/{pincode}`) inside a high-performance HTTP request thread wrapper.
* **1.5s Timeout Guard:** Uses a `SimpleClientHttpRequestFactory` set to 1500ms read/connect timeout to ensure the backend never blocks if the external directory is slow or offline.

#### Step 2: Tier Resolution
* **LOCAL:** If resolved `District` is `"Bhadohi"` or `"Varanasi"`.
* **REGIONAL:** If resolved `State` is `"Uttar Pradesh"`.
* **NATIONAL:** If resolved `State` is any other valid Indian state.

#### Step 3: Local Fallback Engine (Secondary)
If the external postal API lookup fails or times out, the resolver uses a pattern fallback based on the starting digits of the PIN code:
* Starts with `"22"` (Varanasi postal division) -> LOCAL.
* Starts with `"2"` (Uttar Pradesh postal circle) -> REGIONAL.
* Starts with other digits -> NATIONAL.

---

## 💻 3. Frontend Next.js Implementation

### A. Admin SLA Control Board (`marketing/page.tsx`)
Allows admins to update the text values for each tier dynamically. Changes are persisted to the database via `PUT /api/admin/settings/pincode` with secure admin audit trail logging.

### B. Pincode Check Widget (`ProductQuickViewModal.tsx`)
A user-friendly checking input panel is integrated inside the Quick Buy tab:
* Takes a 6-digit number.
* Hits the `/api/public/pincode/check` endpoint.
* Displays a structured green verification box with the matched district/state, expected delivery tier duration, and logistical provider details, or a red error alert if unavailable.
