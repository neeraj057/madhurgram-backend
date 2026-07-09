# Google Maps Autocomplete & Geocoding Boundary Validation (लोकेशन गाइड)

Bhai, is document me Google Maps Integration, Address Autocomplete (with offline fallback simulator), dynamic coordinate mapping, aur backend boundary verification (geocoding) ki poori detail aur testing steps diye gaye hain.

---

## 🌟 Feature Overview

Sweets delivery business me sabse badi samasya galat address aur unreachable pincode hote hain, jiski wajah se delivery fail hoti hai aur return delivery charges merchant ko uthane padte hain. 

Is problem ko permanent solve karne ke liye humne double-layer security banayi hai:
1. **Frontend Layer (Smart Autocomplete):** Google Places JavaScript SDK ke through automatic address suggestion aur details auto-fill. Agar key nahi hai, to **100% Free OpenStreetMap Nominatim Live Autocomplete API** (बिना किसी क्रेडिट कार्ड/बिलिंग सेटअप के) रन होता है, और इंटरनेट इशू होने पर लोकल मीठे बाजारों (Mocks) पर ऑटो-फ़ॉलबैक कर लेता है।
2. **Backend Layer (Geofencing Guard):** Database me Latitude aur Longitude columns add kiye gaye hain. Aur order save karne se pehle coordinates verify kiye jate hain ki delivery location Bharat (India) ke andar hi hai ya nahi.

---

## 🛠️ Technical Design & Code Architecture

### 1. Database & Entity Level (Backend)
- **JPA entities modified:**
  - **[Address.java](file:///d:/MadhurGram/product-service/src/main/java/com/madhurgram/productservice/customer/entity/Address.java):** Added nullable `latitude` and `longitude` double fields.
  - **[Order.java](file:///d:/MadhurGram/product-service/src/main/java/com/madhurgram/productservice/order/entity/Order.java):** Added coordinates to track purchase coordinates separately.
- **DTO schemas updated:**
  - **[AddressDTO.java](file:///d:/MadhurGram/product-service/src/main/java/com/madhurgram/productservice/customer/dto/AddressDTO.java):** Carries coordinates between customer profile requests.
  - **[OrderResponseDTO.java](file:///d:/MadhurGram/product-service/src/main/java/com/madhurgram/productservice/order/dto/OrderResponseDTO.java):** Exposes coordinates for order status maps and logistics triggers.

### 2. Service Layer (Geospatial Bounds Validation)
* **India Boundary coordinates bounds:**
  - Latitude limits: `6.0` to `38.0`
  - Longitude limits: `68.0` to `98.0`
* **File:** [OrderServiceImpl.java](file:///d:/MadhurGram/product-service/src/main/java/com/madhurgram/productservice/order/service/impl/OrderServiceImpl.java#L70-L80)
* **Validation check:**
  ```java
  if (order.getLatitude() != null && order.getLongitude() != null) {
      double lat = order.getLatitude();
      double lng = order.getLongitude();
      if (lat < 6.0 || lat > 38.0 || lng < 68.0 || lng > 98.0) {
          throw new IllegalArgumentException("Delivery address coordinates are outside India's serviceable region.");
      }
  }
  ```

### 3. Frontend Controller & Loader (`CheckoutModal.tsx`)
* **Google SDK Dynamic Loader:**
  - React hook checks if `process.env.NEXT_PUBLIC_GOOGLE_MAPS_API_KEY` is present.
  - Dynamically mounts Google Places libraries and hooks `window.google.maps.places.Autocomplete` on the address input element using standard ref bindings.
* **100% Free Live OpenStreetMap Autocomplete:**
  - Agar Google Key blank hai, to input dynamically **OpenStreetMap (Nominatim)** engine ko request karta hai aur India ke kisi bhi address/village ke real-time suggestions search karke show karta hai.
  - Agar server disconnect ho jaye ya API limit reach ho, to local sweets hubs (Sarafa Bazar, Chappan Dukan, Chandni Chowk, Malleshwaram) mock dataset par fallback kar leta hai.

---

## 🚀 Step-by-Step Testing Guide (इसे टेस्ट कैसे करें)

### Scenario A: Testing Local Autocomplete & Auto-Fill (बिना Google Key के)
1. Frontend aur Backend ko check out karke run karein.
2. Checkout Modal open karein. Apni details bhar kar **Add New Address** par click karein.
3. **Full Address** wale field me keyword type karein: *"Sarafa"* ya *"Chandni"*.
4. Ek glassmorphic dropdown list niche khulegi. Uspar click karein (Jaise: *Sarafa Bazar, Indore, Madhya Pradesh*).
5. **Expected Result:**
   - City field me automatic **Indore** likh jayega.
   - State field me automatic **Madhya Pradesh** likh jayega.
   - Pincode field me automatic **452002** likh jayega.
6. Address details verify karke order submit karein.

---

### Scenario B: Database Inspection (कोऑर्डिनेट्स चेक करना)
1. Order place karne ke baad apne MySQL terminal par check karein:
   ```sql
   SELECT id, customer_name, address, latitude, longitude FROM orders ORDER BY order_date DESC LIMIT 1;
   ```
2. **Expected Result:**
   - Database me coordinate fields blank nahi honge.
   - Sarafa Bazar select kiya tha, to `latitude` field me `22.7196` aur `longitude` field me `75.8577` save milega!

---

### Scenario C: Backend Boundary Security Verification (फ़र्जी एड्रेस ब्लॉकिंग)
Is security check ko test karne ke liye hum Postman ya cURL ke through India ke bahar ke coordinates bhej kar check karenge:
1. Apne CLI par yeh cURL command run karein (isme latitude `40.7128` aur longitude `-74.0060` diya gaya hai jo New York, USA ka hai):
   ```bash
   curl -X POST http://localhost:8080/api/orders/place \
     -H "Content-Type: application/json" \
     -d '{"customerName": "Faker User", "phoneNumber": "9999988888", "address": "Times Square", "pincode": "10001", "cityState": "New York, NY", "totalAmount": 150.00, "latitude": 40.7128, "longitude": -74.0060, "orderItems": [{"productId": 1, "productName": "Cow Ghee", "quantity": 1, "price": 150.00}]}'
   ```
2. **Expected Result:**
   - Server order reject kar dega.
   - **HTTP Status:** `400 Bad Request`
   - **Response Body:**
     ```text
     Delivery address coordinates are outside India's serviceable region.
     ```
   - Database me is order ka entry save nahi hoga!

---

### Scenario D: Real Google Maps API Activation (लाइव करने के लिए)
1. Google Cloud Console me custom project create karein.
2. Bill setup karke **Places API** aur **Maps JavaScript API** ko enable karein.
3. Custom restricted API Key generate karein.
4. Apne Frontend project ke root me `.env.local` file banayein aur usme API Key save karein:
   ```env
   NEXT_PUBLIC_GOOGLE_MAPS_API_KEY=AIzaSyA_REAL_GOOGLE_PLACES_KEY_HERE
   ```
5. Frontend server restart karein (`npm run dev`).
6. Ab address box me search karne par suggestions seedhe Google Maps database se aane shuru ho jayenge!
