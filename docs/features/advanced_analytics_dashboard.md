# Advanced Admin Analytics Console (डैशबोर्ड गाइड)

Bhai, is document me dynamic date range selectors, sales growth comparison metrics, critical low stock detailed grids, aur Redis-powered live active user tracking feature ki implementation aur testing details di gayi hain.

---

## 🌟 Features Overview

Admin panel ko real-world D2C (Direct-to-Consumer) business requirements ke anusaar scalable banane ke liye humne dashboard ko char naye advanced features se upgrade kiya hai:

1. **Live active user tracking (Redis-based):** Live website visitors ki sankhya dashboard par realtime show hoti hai.
2. **Sales Growth Comparison (बिक्री की बढ़त/घटत):** Pichle period ke mukable revenue me growth/loss ka % calculate hota hai.
3. **Dynamic Date Range Range Selector (7, 15, 30 Days):** SVG Charts bina kisi library overhead ke automatic responsive bars aur dynamic grid calculations ke sath range data render karte hain.
4. **Critical Inventory Alert Table:** Low stock item details dashboard par tabular view me show hote hain.

---

## 🛠️ Technical Design & Code Architecture

### 1. Redis Heartbeat Session System (Live Users)
* **Frontend Ping:** Ek dynamic client component `HeartbeatTracker.tsx` root layout me load hota hai. Har 20 seconds me, client browser se server ke endpoint `/api/public/analytics/heartbeat?clientId=xyz` par POST call submit ki jati hai.
* **Server-side TTL Storage:** Backend on request, Redis database me user session key (`active_user_session:xyz`) set karta hai jiska expiry **30 seconds** set hota hai.
* **Active User Count Calculation:** `AnalyticsServiceImpl.java` me active user count Redis me keys pattern fetch karke check karta hai:
  ```java
  java.util.Set<String> keys = redisTemplate.keys("active_user_session:*");
  activeUserCount = keys != null ? Math.max(keys.size(), 1) : 1;
  ```

### 2. Periodical Revenue Comparison (Growth %)
* Ek double period database fetch run kiya jata hai. Agar range `days = 15` hai, to server `days * 2` (yani 30 days) ka sales log pull karta hai.
* **Current Period Revenue (since):** `LocalDate.now().minusDays(days - 1)` se abhi tak ka dynamic revenue.
* **Previous Period Revenue (prevSince):** `LocalDate.now().minusDays((days * 2) - 1)` se `since` date tak ka revenue.
* **Percentage Calculation:**
  $$Growth\% = \frac{CurrentRevenue - PreviousRevenue}{PreviousRevenue} \times 100$$
  *(Handles edge cases like division by zero when previous sales are 0).*

### 3. Catalog Inventory Check (Critical Low Stock)
* Backend query to fetch products with stock <= 5:
  ```sql
  SELECT p FROM Product p WHERE p.stock <= 5 AND p.isActive = true;
  ```
* Low stock products ka standard details list (`LowStockProductDTO.java`) generate hota hai jisme ID, Product Name, price aur actual stock remaining available hota hai.

---

## 🚀 Step-by-Step Testing Guide (टेस्ट कैसे करें)

### Scenario A: Testing Live Active Users (realtime green pulse check)
1. Spring Boot database and frontend dev environments startup run karein.
2. Admin Console open karein aur side-menu se **Analytics** par click karein.
3. **Live Users Card** check karein.
4. **Expected Result:**
   - Card me default **1 Active** green blinking badge ke sath dikhai dega.
   - Dusre browser windows ya incognito tabs me customer website layout (e.g. `http://localhost:3000`) open karein.
   - Dashboard par **Sync Data** click karein (ya 60 seconds auto-refresh ka wait karein), live count scroll hokar tab-counts ke barabar increase ho jayegi!

---

### Scenario B: Dynamic Range Selector (ग्राफ स्केलिंग परीक्षण)
1. Analytics dashboard ke SVG chart section par scroll karein.
2. Card ke top-right corner se range selector dropdown menu par click karein.
3. Choose **15 Days** aur fir **30 Days**.
4. **Expected Result:**
   - Graph card ke bars aur lines dynamic width step size me transform ho jayenge.
   - 30-day range select karne par dates text overlapping control automatic load hogi aur clean interval labels display karegi.

---

### Scenario C: Inventory Alert Table (स्टॉक वार्निंग ग्रिड)
1. Apne local database (MySQL) tool me kisi specific active product ka stock zero (0) ya char (4) set karein:
   ```sql
   UPDATE products SET stock = 3 WHERE id = 1;
   ```
2. Admin analytics page open karein aur metrics refreshes wait karein.
3. **Expected Result:**
   - **Low Stock Alert Card** warning state `COUNT` display badla hua dikhega.
   - Graph section ke strict bottom panel me automatic **Critical Inventory Alert** table appear hogi jisme Product ID `#1`, Sweet Name, Price, aur red alerts display state show honge.

---

### Scenario D: Sales Growth Trend Check
1. Fake checkout orders place karein (ya database tables me historical orders create karein).
2. Analytics dash select parameters verify karein:
   - Agar current week sales previous week sales se behtar hai, to Today's Revenue card me positive growth indicator `+15.0% vs prev period` display hogi.
   - Agar sales decline ho chuki hai, to negative tag check karein.
