# 🌾 MadhurGram Swagger API Testing Guide

This guide details how to launch, authenticate, and test all backend REST APIs for the MadhurGram platform using the interactive Swagger OpenAPI console.

---

## 🔀 Swagger Console Access Link
When the Spring Boot backend server is running on port 8080, open the following URL in your web browser:
👉 **[http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)**

---

## 🔐 How to Test Secured APIs (Step-by-Step JWT Authorization)

Many of our administrative endpoints under `/api/admin/**` and order lists require valid credentials. Follow these steps to authorize your requests in Swagger:

### Step 1: Generate a JWT Token
1. Scroll down to the **auth-controller** section in the Swagger UI.
2. Click on the `POST /api/auth/admin/login` endpoint.
3. Click the **Try it out** button in the top-right of that endpoint block.
4. Replace the request body with one of the following roles:

#### Option A: Super Admin (Can edit prices, settings, delete items)
```json
{
  "username": "admin",
  "password": "MadhurGram@2026"
}
```

#### Option B: Support Staff (Read-only for stats/orders, data masked)
```json
{
  "username": "support",
  "password": "Support@MadhurGram2026"
}
```

5. Click the large blue **Execute** button.
6. Under **Responses**, copy the `token` string value (do NOT copy the double quotes around it, just the inner text starting with `eyJ...`).

---

### Step 2: Supply the Token to Swagger
1. Scroll to the very top of the Swagger page.
2. Click the green **Authorize** (lock icon) button on the right-hand side.
3. In the text input field under **Value**, paste the JWT token string you copied.
4. Click **Authorize**, then click **Close**.

> [!TIP]
> You are now authenticated! All subsequently triggered requests in Swagger will automatically carry the `Authorization: Bearer <your_token>` HTTP header.

---

## 🧪 Testing API Endpoints

### 1. Public APIs (No authorization needed)
These endpoints are open to normal visitors and do not require supply of a JWT token.
* **Testimonials list**: `GET /api/public/feedback/testimonials`
  * Returns active reviews list for the storefront landing page.
* **Customer Feedback Upload**: `POST /api/public/feedback/upload`
  * Upload a product/packaging photo (multipart file) from local disk. Returns image static URL on successful upload.

---

### 2. Admin & Staff APIs (Authorized requests only)
* **Customer CRM Stats**: `GET /api/admin/customers`
  * If logged in as **Support Staff**, phone numbers in the response list will automatically be returned as masked (`+91-XXXXX-1234`).
  * If logged in as **Super Admin**, phone numbers will be shown in raw format.
* **Update Product Price**: `PUT /api/admin/products/{id}`
  * If logged in as **Support Staff**, returns `403 Forbidden` (Support cannot change pricing).
  * If logged in as **Super Admin**, updates the price and records a log inside the database `audit_logs` table.

---

## 🚦 Common Response States

* **`200 OK` / `201 Created`**: Request completed successfully.
* **`400 Bad Request`**: Missing mandatory fields or invalid format details.
* **`401 Unauthorized`**: Token is missing, invalid, or expired.
* **`403 Forbidden`**: Role conflict (e.g. support staff trying to modify pricing).
* **`500 Internal Server Error`**: Unexpected database query or code crash.
