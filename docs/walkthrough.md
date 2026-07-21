# Walkthrough - Redis-Backed API Rate Limiter (Spam Protection)

We have successfully designed and implemented a distributed, memory-efficient **API Rate Limiter** powered by **Redis** to secure the public and sensitive admin endpoints of MadhurGram.

---

## 🛠️ Security Infrastructure Added

### 1. Custom Annotation `@RateLimit`
- **Path**: [RateLimit.java](file:///d:/MadhurGram/product-service/src/main/java/com/madhurgram/productservice/common/annotation/RateLimit.java)
- Allows setting custom request limits and time windows on any controller mapping. E.g. `@RateLimit(limit = 10, windowSeconds = 60)`.

### 2. High-Performance Interceptor `RateLimitInterceptor`
- **Path**: [RateLimitInterceptor.java](file:///d:/MadhurGram/product-service/src/main/java/com/madhurgram/productservice/common/security/RateLimitInterceptor.java)
- Extracts the client's IP address (handling proxies via `X-Forwarded-For` header).
- Generates a Redis key structure: `rate:limit:<client_ip>:<method_name>`.
- Performs an atomic increment in Redis. If the key is new, sets a TTL matching the configuration window.
- Throws a custom `RateLimitExceededException` when thresholds are crossed, aborting execution immediately to prevent load.

### 3. MVC Registration & Exception Mapping
- **Path**: Registered interceptor globally under the `/api/**` route namespace in [WebMvcConfig.java](file:///d:/MadhurGram/product-service/src/main/java/com/madhurgram/productservice/config/WebMvcConfig.java).
- **Path**: Configured a mapper for `RateLimitExceededException` inside [GlobalExceptionHandler.java](file:///d:/MadhurGram/product-service/src/main/java/com/madhurgram/productservice/common/exception/GlobalExceptionHandler.java) to reply with standard JSON and `429 Too Many Requests` status.

---

## 🛡️ Guarded Routes & Limits Configuration

The following critical endpoints have been reinforced with limits:
1. **Order Checkout (`POST /api/orders/place`)**:
   - Limit: **5 requests per minute** per client.
2. **Cart recovery updates (`POST /api/cart/update` and `GET /api/cart/recover`)**:
   - Limit: **10 requests per minute** per client.
3. **Payment webhooks (`POST /api/v1/payments/webhook`)**:
   - Limit: **20 requests per minute** per IP.
4. **Admin Panel logins (`POST /api/auth/admin/login`)**:
   - Limit: **5 attempts per minute** (protects against brute-forcing passwords).

---

## 🔍 How to Verify the Rate Limiter (Local Testing)

1. Boot your spring boot application.
2. Execute 6 consecutive order placement calls within 1 minute via cURL or Postman:
   ```bash
   curl -X POST http://localhost:8080/api/orders/place \
     -H "Content-Type: application/json" \
     -d '{"customerName": "Test Customer", "phoneNumber": "1234567890", "address": "Test street", "pincode": "123456", "cityState": "Test, State", "totalAmount": 100, "orderItems": [{"productId": 1, "productName": "Pure Ghee", "quantity": 1, "price": 100}]}'
   ```
3. **Expected Result**:
   - First 5 requests return successfully (`200 OK` or normal inventory bounds).
   - The 6th request will be blocked immediately with:
     - **HTTP Status**: `429 Too Many Requests`
     - **Response Body**:
       ```json
       {
         "status": "TOO_MANY_REQUESTS",
         "message": "Too many requests. Please try again later."
       }
       ```

---

## 📝 System Audit Logging (Admin Action Logs)

We have built a secure, thread-safe auditing framework to monitor administrative actions performed in the backend.

### 1. Database Table Schema (`audit_logs`)
- Stored fields:
  - `id`: Auto-incrementing primary key.
  - `username`: Active administrator executing the command (resolves from Spring Security principal).
  - `action`: Categorized operation string (e.g. `UPDATE_ORDER_STATUS`, `UPDATE_PRODUCT`).
  - `entity_id`: ID of target record (Order ID, Product ID, etc.).
  - `details`: Rich text description of changes (e.g., old status -> new status, product name, price/stock adjustments).
  - `ip_address`: Dynamic client IP address extracted from servlet request context.
  - `created_at`: LocalDateTime timestamp.

### 2. Instrumented Operations
The following actions trigger immediate audit logs:
- **Order Lifecycle updates**: Transitioning status updates (`OrderServiceImpl.java`).
- **Product catalog updates**: Adding new items, editing details (price/stock), or deleting items (`AdminProductController.java`).
- **Auto-Pilot Recovery settings**: Toggling cart auto-pilot setting ON or OFF (`AdminSettingsController.java`).
- **Cart deletions**: Manually purging recovery files (`AdminAbandonedCartController.java`).

### 3. How to Verify
1. Boot your spring boot application.
2. Perform any administrative task (such as toggling Auto-pilot in the admin UI or updating a product price).
3. Connect to your MySQL database console and query the audit table:
   ```sql
   SELECT * FROM audit_logs ORDER BY created_at DESC;
   ```
4. Confirm the entry displays `admin` (username), client IP, action keyword, and details description correctly!

---

## 💳 Dynamic Payment Method Selection (Prepaid vs. COD)

We have integrated a premium checkout selector that lets users choose between **Online Payment (Prepaid)** and **Cash on Delivery (COD)**.

### 1. Checkout UI Component
- **Path**: [CheckoutModal.tsx](file:///c:/Users/victus/madhurgram-frontend/src/components/features/checkout/CheckoutModal.tsx)
- Dynamically displays selection buttons with gold accents.
- If **Online Payment** is selected, sends `paymentStatus = "PENDING"` and `orderStatus = "PENDING"`.
- If **COD** is selected, sends `paymentStatus = "COD"` and `orderStatus = "CONFIRMED"`.

### 2. Auto-Confirmation & Auto-Logistics on Backend
- **Path**: [OrderServiceImpl.java](file:///d:/MadhurGram/product-service/src/main/java/com/madhurgram/productservice/order/service/impl/OrderServiceImpl.java)
- In `placeOrder()`, COD orders are initialized immediately as `CONFIRMED`.
- This triggers:
  - An audit log entry: `PLACE_ORDER_COD`.
  - A WhatsApp confirmation message.
  - Decoupled `OrderConfirmedEvent` to trigger Delhivery logistics pickup asynchronously.

### 3. Smart Tracking Alert View
- **Path**: [page.tsx](file:///c:/Users/victus/madhurgram-frontend/src/app/orders/track/[id]/page.tsx)
- COD orders hide the Stripe/Razorpay payment simulator warnings and display a clean green success badge: *"Payment Method: Cash on Delivery (COD)"*.

### 4. How to Verify
1. Open the checkout modal and select **Cash on Delivery**.
2. Submit the order.
3. Observe the tracking page. The order status will immediately show **Confirmed** (or Shipped if logistics runs fast) without showing any payment retry warning banner.
4. Verify in the database that the order is registered with `payment_status = 'COD'` and `order_status = 'CONFIRMED'`.

---

## 💳 Direct Checkout Payment Simulation (Stripe / Razorpay Overlay)

We have updated the prepaid checkout flow to show a secure payment simulation popup **directly inside the checkout modal** when they click "Place Order".

### 1. Unified Intercept in Checkout
- **Path**: [CheckoutModal.tsx](file:///c:/Users/victus/madhurgram-frontend/src/components/features/checkout/CheckoutModal.tsx)
- When **Online Prepaid** is selected, clicking "Place Order" halts order creation and opens a secure **Simulated Payment Gateway** interface within the modal itself.
- **Pay Button (Simulate Success)**:
  - Generates a dummy transaction ID and posts the payload to backend with `paymentStatus = "COMPLETED"`, `orderStatus = "CONFIRMED"`.
  - Places the order and redirects straight to the success screen and tracker page as **Confirmed/Shipped**.
- **Cancel Button (Simulate Failure)**:
  - Aborts payment. The simulator overlay closes, keeping the checkout modal open.
  - Displays an inline red warning: *"Payment was cancelled. You can retry or select Cash on Delivery."*
  - This preserves all client input fields (phone, name, address) so they can retry or switch to COD seamlessly.

### 2. How to Verify
1. Select **Online Prepaid** in Checkout and click **Place Order**.
2. Confirm that a secure mock gateway popup appears overlaying the checkout modal.
3. Click **Cancel (Simulate Failure)**. Confirm that the simulator closes, the main checkout modal remains open, and the red alert is visible.
4. Click **Place Order** again, and this time select **Pay**. Confirm that the order is placed successfully as `CONFIRMED` with status `COMPLETED`, and redirects immediately to the tracking page with a green verified tag.

---

## 📈 Search Engine Optimization (SEO) & System Health Monitoring

We have successfully prepared the platform for public discoverability and real-time operations visibility.

### 1. Dynamic Sitemap & Crawl Controls (SEO)
- **robots.txt**: Configured via Next.js metadata route [robots.ts](file:///c:/Users/victus/madhurgram-frontend/src/app/robots.ts). Allows search indexers to crawl the store while keeping dashboard management routes (`/admin/**`) hidden.
- **sitemap.xml**: Configured via dynamic [sitemap.ts](file:///c:/Users/victus/madhurgram-frontend/src/app/sitemap.ts). Fetches products on demand from the backend to automatically build standard XML URLs, ensuring Google indices products seamlessly.

### 2. JVM & Endpoint metrics watchtower (Monitoring)
- Exposes JVM status, query execution, API counts, and system metrics directly from the Spring Boot actuator container.
- Exposes metrics in standard Prometheus formatting ready to hook into Prometheus scraping servers & Grafana dashboards.
- Exposed routes:
  - **Health Endpoint**: `http://localhost:8080/actuator/health` (Returns status `"UP"`).
  - **Prometheus Scraper**: `http://localhost:8080/actuator/prometheus` (Returns Micrometer counters).

### 3. How to Verify
1. Start your local Next.js client and Spring Boot server.
2. In your browser, open:
   - `http://localhost:3000/robots.txt` - Confirm rules allow `/` and deny `/admin/`.
   - `http://localhost:3000/sitemap.xml` - Confirm details dynamically list the active product endpoints.
   - `http://localhost:8080/actuator/health` - Confirm backend health status reads `"UP"`.
   - `http://localhost:8080/actuator/prometheus` - Confirm raw metric counters are rendered cleanly.

---

## 📊 Visualizing Metrics with Local Prometheus & Grafana (Docker)

You can run a local monitoring stack to visualize the JVM health metrics using Docker.

### 1. File Configuration
- **Path**: [prometheus.yml](file:///d:/MadhurGram/product-service/monitoring/prometheus.yml) (Scrape targets)
- **Path**: [docker-compose.yml](file:///d:/MadhurGram/product-service/monitoring/docker-compose.yml) (Orchestration details)

### 2. How to Start the Containers
Open a terminal in the monitoring directory and run:
```bash
cd d:\MadhurGram\product-service\monitoring
docker-compose up -d
```

### 3. Visual Dashboard Verification
- **Prometheus Dashboard**: Open `http://localhost:9090`. Type `process_cpu_usage` or `jvm_memory_used_bytes` in the expression box and click **Graph** to view the raw metrics charts.
- **Grafana Visualization**:
  1. Open `http://localhost:4000` (Login: `admin` / `admin`).
  2. Add **Prometheus** as a data source and set the connection URL to `http://prometheus:9090`.
  3. Import a standard Spring Boot dashboard (Dashboard ID: `4701` or `11378`) to view beautiful Grafana charts.

---

## 🗺️ Google Maps Location Autocomplete & Geocoding Validation

We have added dynamic location autocompletion to the shipping forms to ensure accurate delivery coordinates, alongside geofencing security validation on the backend.

### 1. Unified Autocomplete System (Frontend)
- **Path**: [CheckoutModal.tsx](file:///c:/Users/victus/madhurgram-frontend/src/components/features/checkout/CheckoutModal.tsx)
- **Official Google Places SDK Integration**: If `NEXT_PUBLIC_GOOGLE_MAPS_API_KEY` is set in `.env.local`, the script is dynamically loaded, and the input automatically maps to Google Places Autocomplete.
- **Smart Offline Fallback Dropdown**: If the API key is missing, typing 3+ characters searches a local index of prominent Indian sweets markets (e.g. *Sarafa Bazar, Chappan Dukan, Chandni Chowk*). Selecting a suggestion auto-populates the City, State, Pincode, and sets mock coordinate markers (Latitude & Longitude).

### 2. Geographical Boundary Guard (Backend)
- **Path**: [OrderServiceImpl.java](file:///d:/MadhurGram/product-service/src/main/java/com/madhurgram/productservice/order/service/impl/OrderServiceImpl.java#L70-L80)
- Checks the order coordinates during checkout.
- Blocks any order submission containing coordinates outside the bounding box of India (Latitude: `6.0` to `38.0`, Longitude: `68.0` to `98.0`).
- Throws an `IllegalArgumentException("Delivery address coordinates are outside India's serviceable region.")` for out-of-bounds orders.

### 3. How to Verify
1. **Mock Verification**:
   - Start Next.js and Spring Boot.
   - Click **Add New Address** during checkout.
   - Type *"Sarafa"* in the Full Address field.
   - Select *"Sarafa Bazar, Indore, Madhya Pradesh"* from the dropdown suggestion.
   - Confirm that City (*Indore*), State (*Madhya Pradesh*), and Pincode (*452002*) are filled automatically.
   - Complete checkout. Query the `addresses` or `orders` tables in your database to verify coordinates `latitude = 22.7196` and `longitude = 75.8577` were saved.
2. **Boundary Validation Verification**:
   - Submit a POST request to `/api/orders/place` with coordinates outside India (e.g., `latitude = 40.7128`, `longitude = -74.0060` - New York).
   - Verify the backend rejects the order with `400 Bad Request` and message: *"Delivery address coordinates are outside India's serviceable region."*
