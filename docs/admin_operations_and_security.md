# MadhurGram Admin Operations & Security Blueprint

This document outlines the system architecture, order lifecycle, invoice eligibility rules, and security access controls for the MadhurGram backend and frontend dashboards.

---

## 1. Order Lifecycle & Status Management

Orders placed in the system flow through different state transitions depending on their payment method:

| Payment Method | Initial Status | Transition Path | Trigger Events |
| :--- | :--- | :--- | :--- |
| **Cash on Delivery (COD)** | `CONFIRMED` | `CONFIRMED` $\rightarrow$ `SHIPPED` $\rightarrow$ `OUT_FOR_DELIVERY` $\rightarrow$ `DELIVERED` | Automatically confirmed on placement. Stock is reserved immediately. |
| **Online Prepaid** | `PENDING` | `PENDING` $\rightarrow$ `CONFIRMED` $\rightarrow$ `SHIPPED` $\rightarrow$ $\dots$ | Starts as `PENDING`. Updates to `CONFIRMED` only when payment gateway webhook verifies payment success. |

### Valid Status Transitions
To protect data integrity, the system implements a strict state machine validated at the database transaction layer:
* **PENDING** $\rightarrow$ `CONFIRMED` or `CANCELLED`
* **CONFIRMED** $\rightarrow$ `SHIPPED` or `CANCELLED`
* **SHIPPED** $\rightarrow$ `OUT_FOR_DELIVERY` or `CANCELLED`
* **OUT_FOR_DELIVERY** $\rightarrow$ `DELIVERED` or `CANCELLED`
* **DELIVERED** / **CANCELLED** $\rightarrow$ Final states (No further transitions allowed).

---

## 2. Tax Invoice Generation Rules

MadhurGram enforces strict legal compliance on GST tax invoices. Admin download permission is dynamically granted based on order status:

* **Download Allowed Stage:** `CONFIRMED`, `SHIPPED`, `OUT_FOR_DELIVERY`, and `DELIVERED`.
* **Download Blocked Stage:**
  * `PENDING`: No invoice can legally be generated before an order is confirmed (either via prepaid payment success or manual COD verification).
  * `CANCELLED`: Invoices are voided upon cancellation.

### Dynamic Tax & HSN Calculation
Invoices dynamically pull details from the backend's real-time tax slabs (e.g., Ghee at 12% GST, Oils at 5% GST). Old orders with missing tax rows utilize a defensive fallback logic of `totalAmount / 1.05` to prevent rendering crashes.

---

## 3. Data Protection & Role-Based Masking

To maintain compliance and protect customer PII (Personally Identifiable Information), role-based phone number masking is enforced at the controller serialization level:

### Access Matrices

| Role Authority | Display Format | Access Permissions |
| :--- | :--- | :--- |
| **SUPER_ADMIN** (`ROLE_SUPER_ADMIN` or `SUPER_ADMIN`) | `1234567890` (Full Real Phone Number) | Full access to customer history, tax rules configuration, and unmasked analytics. |
| **SUPPORT_STAFF** (`ROLE_SUPPORT_STAFF`) | `+91-XXXXX-7890` (Masked Phone Number) | Can coordinate logistics and manage shipping, but cannot view full customer phone numbers or modify tax policies. |

> [!NOTE]
> **Token Compatibility Layer:** 
> The system checks for both `ROLE_SUPER_ADMIN` and plain `SUPER_ADMIN` authorities. This prevents masking issues for administrators holding legacy active session tokens in their browser's `localStorage` from older deployments.

---

## 4. Run-Time Operations & Performance Guidelines

### Low-Memory Execution Guard
If running on systems with limited RAM, start the backend process with bounded JVM heap parameters to prevent Out of Memory (OOM) crashes:

```powershell
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xmx384m -XX:MaxDirectMemorySize=64m"
```

### Lazy Loading & Serialization
All transactional updates that fetch orders eagerly initialize their associated `orderItems` (via size calls) before closing the JPA transaction context. This prevents `LazyInitializationException` from returning generic `500 Failed to write request` payloads to the frontend.
