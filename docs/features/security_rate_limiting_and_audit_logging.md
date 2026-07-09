# Feature Documentation: Rate Limiting & Admin Audit Logging (API Security Engine)

Bhai, is document me simple Hinglish/WhatsApp language me likha hai ki ye naya Security Rate Limiting aur Admin Audit Logging system back-end me kaise kaam karta hai, iski testing kaise karni hai, aur future me agar changes karne ho to kaise karenge.

---

## 1. Overview (Ye Features Kyu Lagaye Gaye?)

Site ko launch/live karne se pehle use secure aur professional banana sabse zaroori hai. Uske liye humne do major security features dale hain:
1. **Rate Limiting (Spam Control):** Public/open endpoints (jaise checkouts, cart recovery updates) par spam bots ya malicious users ko dher saare fake orders/carts banakar server crash karne ya Twilio bills badhane se rokne ke liye.
2. **Audit Logging (Activity Tracking):** Jab multiple admins panel chalayein, to safety aur tracking ke liye database me automatic logs record hote hain ki *kis admin ne, kis IP address se, kis time par kya change kiya*.

---

## 2. Rate Limiting System (Kaise Kaam Karta Hai?)

### A. Core Architecture Flow
1. **Custom Annotation `@RateLimit`:** Humne ek annotation banaya hai jisko kisi bhi controller method par lagakar hum limit aur timer window specify kar sakte hain.
2. **Redis-Backed Interceptor (`RateLimitInterceptor`):**
   * Spring MVC ke request processing flow me ye interceptor har `/api/**` request ko intercept karta hai.
   * Client ka IP address extract karta hai (agar Nginx/Cloudflare proxy hai to `X-Forwarded-For` header check karta hai).
   * Redis me counter key banata hai: `rate:limit:<client_ip>:<method_name>`.
   * Increment command chalta hai. Agar key pehli baar bani hai, to dynamic Time-to-Live (TTL) set kar deta hai.
   * Agar counter configured limit ko cross kar jata hai, to exception throw karta hai jo automatic `429 Too Many Requests` status return karti hai.

### B. Endpoints protected & Configured Limits:
* **Checkout/Place Order:** 1 minute me max **5 requests** per IP.
* **Cart Updates/Recover:** 1 minute me max **10 requests** per IP.
* **Payment Webhook:** 1 minute me max **20 requests** per IP.
* **Admin Login:** 1 minute me max **5 attempts** per IP (password brute-forcing se bachane ke liye).

### C. Testing Guide (cURL test)
Terminal me checkouts ko spam karke check karein:
```bash
# Lagaatar 6 baar ye curl command chalayein (1 minute ke andar)
curl -X POST http://localhost:8080/api/orders/place \
  -H "Content-Type: application/json" \
  -d "{\"customerName\": \"Test\", \"phoneNumber\": \"8779999666\", \"address\": \"Google\", \"pincode\": \"221303\", \"cityState\": \"Pune, UP\", \"totalAmount\": 649.00, \"orderItems\": [{\"productId\": 1, \"productName\": \"Cow Ghee\", \"quantity\": 1, \"price\": 649.00}]}"
```
* **Expected Result:** Pehli 5 calls pass hongi. 6th call pe automatic error response aayega:
  * **HTTP Status:** `429 Too Many Requests`
  * **Response Body:**
    ```json
    {
      "status": "TOO_MANY_REQUESTS",
      "message": "Too many requests. Please try again later."
    }
    ```

---

## 3. System Audit Logging (Admin Action Logger)

### A. Under the Hood Flow
1. **JPA Entity & Table:** database me `audit_logs` table bani hai.
2. **Dynamic Context Extraction:**
   * Jab bhi koi action audit service ko pass hota hai, ye auto-pilot mode me Spring Security Context check karke active Admin username (e.g., `admin`) fetch kar leta hai.
   * Client ka IP address current thread request context se resolve kar leta hai.
3. **Database logs details:**
   * `username`: Kis user ne change kiya (logged-in session).
   * `action`: Action ka short code (e.g., `UPDATE_ORDER_STATUS`, `UPDATE_PRODUCT`).
   * `entity_id`: Product ID, Order ID, etc.
   * `details`: Human-readable description (e.g., *"Status transitioned from PENDING to CONFIRMED"*).
   * `ip_address`: Kis machine se request aayi thi.
   * `created_at`: Exact event timestamp.

### B. Triggered Events
Audit logs in methods par capture hote hain:
* `OrderServiceImpl.updateOrderStatus()` -> Order status change hone par.
* `AdminProductController.addProduct()` -> Naya product catalog me add karne par.
* `AdminProductController.updateProduct()` -> Product details, price ya stock change karne par.
* `AdminProductController.deleteProduct()` -> Product delete karne par.
* `AdminSettingsController.setAutoRecoveryStatus()` -> Cart recovery ka auto-pilot state toggling karne par.
* `AdminAbandonedCartController.deleteAbandonedCart()` -> Manual cart recovery records delete karne par.

### C. Testing Guide (DB validation)
1. Admin panel me jao aur kisi Product ka price update karo ya Cart auto-pilot switch toggle karo.
2. Database console open karke query run karo:
   ```sql
   SELECT * FROM audit_logs ORDER BY created_at DESC LIMIT 5;
   ```
3. **Expected Result:** Aapko audit record dikhega, jisme username `admin`, unka IP address, target product/order id, aur details me changes ka exact trace record hoga.

---

## 4. Future Extensibility Guide (Dev guidelines for future changes)

Bhai, agar future me changes karne hon, to in simple rules ko follow karein:

### A. Naye Controller par Rate Limiting kaise lagayein?
Agar aap koi naya controller method banate hain aur chahte hain ki usper rate limit lage, to bas method ke upar `@RateLimit` annotation laga dein. E.g.:
```java
@GetMapping("/search")
@RateLimit(limit = 15, windowSeconds = 60) // 1 minute me max 15 requests
public ResponseEntity<?> searchProducts(@RequestParam String query) {
    ...
}
```
Core config change karne ki bilkul zaroori nahi hai!

### B. Naye Controller/Service me Audit Logging kaise karein?
Agar naya controller/service audit trail me add karna ho:
1. Apni Controller/Service class me `AuditLogService` ko inject karein (Autowired or Constructor injection).
2. Service method `log()` ko call karein:
   ```java
   auditLogService.log("ACTION_NAME", String.valueOf(targetId), "Details of what changed");
   ```
   *E.g., login success hone par:*
   `auditLogService.log("ADMIN_LOGIN", null, "Admin logged in successfully");`

Ye system modular hai, jisse dynamic configurations maintain rahengi bina kisi code dependency ya regression side-effects ke!
