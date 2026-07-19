# MadhurGram — Pincode Delivery Serviceability Architecture

This document details the architecture, configuration, and flow of the **Pincode Delivery SLA & Courier Availability Checker** feature.

---

## 1. Feature Overview

When a customer enters their PIN code on the Product Quick View Modal, the system dynamically resolves:
1. **Availability:** Does MadhurGram deliver to this PIN code?
2. **SLA (ETA):** Estimated delivery timeframe.
3. **COD Status:** Is Cash On Delivery allowed?
4. **Courier Name:** Which logistics partner will handle the delivery.

---

## 2. Architecture — 3-Phase System

The backend implements a highly resilient 3-Phase verification engine:

```text
Customer enters pincode (Frontend)
           │
           ▼
GET /api/public/pincode/check?pincode=XXXXXX
           │
           ├─► Phase 2: SHIPROCKET_SERVICEABILITY_ENABLED = true?
           │       │
           │       ├─► YES → ShiprocketServiceabilityService.checkServiceability()
           │       │               │
           │       │               ├─► Redis Cache HIT?  → Return cached result (Phase 3)
           │       │               │
           │       │               └─► Cache MISS → ShiprocketAuthService.getToken()
           │       │                                   → Shiprocket API call
           │       │                                   → Cache result 6h in Redis
           │       │                                   → Return real-time result
           │       │
           │       └─► NO / API failure → Phase 1 Fallback
           │
           └─► Phase 1 Fallback (Zone based estimation)
                   │
                   ├─► api.postalpincode.in → district/state lookup
                   │       Bhadohi/Varanasi → LOCAL tier
                   │       Uttar Pradesh   → REGIONAL tier
                   │       Rest of India   → NATIONAL tier
                   │
                   └─► Postal API timeout → Digit-pattern fallback
                           Starts "22" → LOCAL
                           Starts "2"  → REGIONAL
                           Others      → NATIONAL
```

---

## 3. Project Structure (Logistics Module)

The feature rigorously adheres to standard controller-service-impl-provider architecture:

- `logistics/controller/PincodeSlaController.java`: Routes requests to service layer.
- `logistics/dto/`: Holds standard `PincodeSlaResponseDto`, `AdminPincodeSettingsDto`, `ShiprocketSettingsDto`.
- `logistics/service/PincodeSlaService.java`: Service interface for SLA logic.
- `logistics/service/impl/PincodeSlaServiceImpl.java`: Orchestrates fallback chains and zone tier mapping.
- `logistics/provider/ShiprocketAuthService.java`: Interface for token lifecycle.
- `logistics/provider/impl/ShiprocketAuthServiceImpl.java`: Memory-caches Shiprocket JWT for 9 days, auto-refreshes on 401.
- `logistics/provider/ShiprocketServiceabilityService.java`: Interface for external logistics check.
- `logistics/provider/impl/ShiprocketServiceabilityServiceImpl.java`: Hits external API and processes ratings.

---

## 4. Phase 1 — Honest Zone-Based Estimation (Fallback)

If Shiprocket is disabled, or external APIs fail, the system falls back to honest approximations:

| Tier | Condition | SLA (Configurable) | COD | Courier |
|------|-----------|--------------------|-----|---------|
| **LOCAL** | District = Bhadohi / Varanasi | 1-2 Business Days | ✅ Yes | Shiprocket / Self |
| **REGIONAL** | State = Uttar Pradesh | 2-3 Business Days | ✅ Yes | Shiprocket Express |
| **NATIONAL** | Rest of India | 4-6 Business Days | ❌ No | Delhivery / BlueDart |

---

## 5. Phase 2 & 3 — Real-Time Shiprocket Integration & Caching

### Auth Token Lifecycle
Shiprocket requires JWT tokens (valid 10 days). `ShiprocketAuthServiceImpl` maintains a token in active memory with a **9-day TTL**, automatically fetching a fresh one when necessary or encountering a `401 Unauthorized` response.

### Serviceability & Rating Sorting
`ShiprocketServiceabilityServiceImpl` hits the courier endpoint. Shiprocket returns multiple courier options. The backend maps through the response and selects the **highest-rated** courier company to display to the user, parsing the `etd` into standard business days.

### Redis Caching (6-Hour TTL)
Pincode availability rarely changes intraday. To save API quota and accelerate UX, `CacheConfig.java` defines a specific `pincodeServiceability` cache with a **6-hour TTL**. 
- Repeated lookups for the same pincode take ~2ms instead of ~400ms.
- Safe `CacheErrorHandler` integration ensures that if Redis goes offline, the system degrades gracefully and calls the API directly.

---

## 6. Testing Guide

### A. Frontend Manual Testing (Simple UX Test)
To verify exactly what the customer sees on the website right now (Zone Logic / Phase 1 is active by default):

1. Go to `http://localhost:3000` (Next.js frontend).
2. Click **Quick View** on any product.
3. Switch to the **Quick Buy** tab.
4. **Test LOCAL Tier:** 
   - Enter Pincode: `221303`
   - *Expected:* "Estimated delivery: 1-2 Business Days" | `COD Available` badge | Courier: Shiprocket Express / Self Delivery
5. **Test REGIONAL Tier:** 
   - Enter Pincode: `226001` (Lucknow)
   - *Expected:* "Estimated delivery: 2-3 Business Days" | `COD Available` badge | Courier: Shiprocket Express
6. **Test NATIONAL Tier:** 
   - Enter Pincode: `400001` (Mumbai)
   - *Expected:* "Estimated delivery: 4-6 Business Days" | `Prepaid Only` badge (COD not available)
7. **Test INVALID Pincode:** 
   - Enter Pincode: `000000`
   - *Expected:* Red error box saying delivery is currently not available.

*Note: Phase 2 (Real Shiprocket Courier Check) will automatically activate on the UI once you add your actual Shiprocket Email/Password in the Admin Panel before going live.*

### B. Backend API Testing (Zone Logic)
Ensure `SHIPROCKET_SERVICEABILITY_ENABLED = false` in Admin panel.
```bash
# Test Local Tier (Bhadohi)
curl "http://localhost:8080/api/public/pincode/check?pincode=221303"
# Expected: tier=LOCAL, cod=true, sla="1-2 Business Days"

# Test National Tier (Mumbai)
curl "http://localhost:8080/api/public/pincode/check?pincode=400001"
# Expected: tier=NATIONAL, cod=false, sla="4-6 Business Days"
```

### C. Backend API Testing (Shiprocket API & Redis)
Enable Shiprocket via Admin API:
```bash
curl -X PUT http://localhost:8080/api/admin/settings/shiprocket \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <admin_token>" \
  -d '{
    "shiprocketEmail": "your@email.com",
    "shiprocketPassword": "password",
    "shiprocketPickupPincode": "221303",
    "shiprocketEnabled": "true"
  }'
```

Run check on a real pincode. The **second call** will be instantly served from Redis cache.
```bash
curl "http://localhost:8080/api/public/pincode/check?pincode=110001"
# Expected: Real courier name (e.g. "Delhivery"), real ETA, real COD status from Shiprocket
```

Validate Redis TTL (6 Hours):
```bash
redis-cli
> KEYS pincodeServiceability::*
> GET "pincodeServiceability::110001"
> TTL "pincodeServiceability::110001"
```

---

## 7. Database Configurations (`system_settings`)

| Key | Default | Description |
|-----|---------|-------------|
| `PINCODE_SLA_LOCAL` | `1-2 Business Days` | SLA for Bhadohi/Varanasi |
| `PINCODE_SLA_REGIONAL` | `2-3 Business Days` | SLA for Uttar Pradesh |
| `PINCODE_SLA_NATIONAL` | `4-6 Business Days` | SLA for rest of India |
| `SHIPROCKET_SERVICEABILITY_ENABLED` | `false` | Master toggle for real-time check |
| `SHIPROCKET_EMAIL` | *(empty)* | Login credential |
| `SHIPROCKET_PASSWORD` | *(empty)* | Login credential |
| `SHIPROCKET_PICKUP_PINCODE` | `221303` | MadhurGram Warehouse location |
