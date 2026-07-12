# MadhurGram Master Go-Live & Future Scaling Blueprint (लॉन्च और स्केलिंग मास्टर गाइड)

नमस्ते! एक **Solution Architect** के रूप में मैंने हमारे द्वारा चर्चा किए गए सभी महत्वपूर्ण पहलुओं को इस सिंगल मास्टर ब्लूप्रिंट में संकलित (compile) कर दिया है। यह दस्तावेज़ आपके पहले लॉन्च से लेकर भविष्य में लाखों कस्टमर्स तक स्केल करने में आपका मार्गदर्शन करेगा।

---

## 📋 Table of Contents (विषय सूची)
1. **Current Codebase Status (वर्तमान स्थिति)**
2. **Environment & Hosting Architecture (पर्यावरण और होस्टिंग संरचना)**
3. **Step-by-Step Staging Setup (स्टेजिंग एनवायरनमेंट बनाने के कदम)**
4. **Load Testing Checklist (लोड टेस्टिंग और परफॉरमेंस चेक)**
5. **Stripe/Razorpay & Delhivery Live Switch (असली गेटवे को सक्रिय करना)**
6. **Future Expansion Blueprint (भविष्य का क्लाउड स्केलिंग रोडमैप)**
   * *Option A: Render Persistent Volumes*
   * *Option B: Cloudinary Migration Plan (Free Tier)*

---

## 🔍 1. Current Codebase Status (वर्तमान स्थिति)

हमारा कोड लाइव जाने के लिए संरचनात्मक रूप से (Structurally) 100% तैयार है:
* **Compilation:** Backend (Spring Boot + Java 21) और Frontend (Next.js 16) दोनों बिना किसी एरर के सफलतापूर्वक कंपाइल और बिल्ड हो रहे हैं।
* **Central API Manager:** हमने फ्रंटएंड के सभी hardcoded URLs को हटाकर उन्हें centralized `API_ENDPOINTS` ([api.ts](file:///c:/Users/victus/madhurgram-frontend/src/apis/api.ts)) और dynamic config variables में बदल दिया है।
* **Sitemap & SEO:** `sitemap.ts` और `robots.ts` अब `NEXT_PUBLIC_APP_URL` का उपयोग करके डोमेन को डायनामिक रूप से रिज़ॉल्व करते हैं।
* **Mock Status:** भुगतान (Stripe/Razorpay) और लॉजिस्टिक्स (Delhivery/Shiprocket) अभी **Test/Mock mode** में हैं। Twilio SMS भी **Mock mode** (कंसोल लॉगिंग) में है।

---

## 🌐 2. Environment & Hosting Architecture (हम कहाँ होस्ट करेंगे?)

हम बहुत ही सरल, सुरक्षित और **किफ़ायती (Free Tier / Low Cost)** आर्किटेक्चर का उपयोग करेंगे:

| Component | Technology | Hosting Platform | Cost (Dev/Test) | Action in Production |
| :--- | :--- | :--- | :--- | :--- |
| **Frontend** | Next.js (React) | **Vercel** | **₹0 (Free)** | Connect `madhurgram.com` |
| **Backend** | Spring Boot | **Render** / **Railway** | **₹0 - $7/mo** | Scale RAM to 512MB/1GB |
| **Database** | MySQL | **Render Managed DB** | **$7/mo** | Upgrade storage limits |
| **Cache & Limiter**| Redis | **Upstash Redis** | **₹0 (Free)** | Use standard production Redis |
| **SMS/WhatsApp** | Twilio REST API | **Twilio Sandbox** | **Free Trial** | Buy Live Business Sender Number |

---

## 🚀 3. Step-by-Step Staging Setup (स्टेजिंग सर्वर बनाने के कदम)

लॉन्च से 15 दिन पहले हम एक **Staging Server** बनाएंगे। इसके कदम निम्नलिखित हैं:

### Step 3.1: Database Setup
1. [Render.com](https://render.com) पर साइन-अप करें।
2. **New +** पर क्लिक करें और **PostgreSQL / MySQL** डेटाबेस बनाएं।
3. डेटाबेस बनने के बाद उसकी **Internal Database URL** और **External Connection String** को कॉपी कर लें।

### Step 3.2: Deploy Backend to Render (Docker)
1. Render पर **Web Service** बनाएं और उसे अपने GitHub repository से लिंक करें।
2. Render अपने आप प्रोजेक्ट में मौजूद `Dockerfile` को डिटेक्ट कर लेगा।
3. **Environment Variables** सेक्शन में निम्नलिखित सेटिंग्स जोड़ें:
   ```env
   SPRING_PROFILES_ACTIVE=dev
   SPRING_DATASOURCE_URL=jdbc:mysql://your-render-db-host:3306/madhurgram_db
   SPRING_DATASOURCE_USERNAME=your_db_username
   SPRING_DATASOURCE_PASSWORD=your_db_password
   JWT_SECRET=your_dev_secret_key_32_characters
   REDIS_HOST=your_redis_host
   ```
4. **Deploy** पर क्लिक करें। Render कंटेनर बिल्ड करके आपको एक लाइव API URL देगा (उदा. `https://madhurgram-backend.onrender.com`)।

### Step 3.3: Deploy Frontend to Vercel
1. [Vercel.com](https://vercel.com) पर साइन-अप करके अपने GitHub को लिंक करें।
2. `madhurgram-frontend` प्रोजेक्ट को इम्पोर्ट करें।
3. **Environment Variables** में ये जोड़ें:
   ```env
   NEXT_PUBLIC_API_BASE_URL=https://madhurgram-backend.onrender.com
   NEXT_PUBLIC_APP_URL=https://madhurgram-staging.vercel.app
   ```
4. **Deploy** पर क्लिक करें। आपकी टेस्टिंग वेबसाइट लाइव हो जाएगी!

---

## ⚡ 4. Load Testing Checklist (लोड टेस्टिंग और परफॉरमेंस चेक)

लाइव करने से पहले हमें देखना है कि क्या सर्वर 1000 यूज़र्स का लोड संभाल सकता है:
1. अपने लैपटॉप पर `k6` टूल इंस्टॉल करें (`choco install k6`).
2. कमांड प्रॉम्प्ट खोलें और हमारे द्वारा बनाई गई स्क्रिप्ट रन करें:
   ```bash
   # Staging URL पर लोड टेस्ट रन करना
   k6 run -e TARGET_URL=https://madhurgram-backend.onrender.com load-test.js
   ```
3. **क्या चेक करना है?**
   * **Error Rate:** 1% से कम होना चाहिए (`http_req_failed` metric)।
   * **Response Time:** 95% रिक्वेस्ट्स 300ms से कम में पूरी होनी चाहिए (`http_req_duration` metric)।
   * यदि डेटाबेस कनेक्शन एरर आता है, तो Render में `Database Connection Pool` (HikariCP) के साइज को बढ़ाएं।

---

## 💳 5. Stripe/Razorpay & Delhivery Live Switch (असली गेटवे चालू करना)

जब आप कस्टमर से असली पैसे लेने के लिए तैयार हों:

### Step 5.1: Razorpay/Stripe SDK Active Code
अभी फ्रंटएंड के [CheckoutModal.tsx](file:///c:/Users/victus/madhurgram-frontend/src/components/features/checkout/CheckoutModal.tsx) में simulated popup चलता है। लाइव करने के लिए:
1. मर्चेंट क्रेडेंशियल्स डालें।
2. सिम्युलेटर कोड `showPaymentSimulator` को हटाकर असली SDK को एक्टिवेट करें:
   ```javascript
   // Razorpay integration example
   const rzp = new window.Razorpay({
     key: "rzp_live_your_live_key",
     amount: order.totalAmount * 100, // paise
     currency: "INR",
     name: "MadhurGram",
     handler: function (response) {
       // Send response.razorpay_payment_id to backend for order confirmation
     }
   });
   rzp.open();
   ```

### Step 5.2: Live API credentials swap
सर्वर सेटिंग्स (Render / AWS Env Variables) में जा कर **Test/Mock keys** को **Production Keys** से बदल दें:
```env
# Change from Mock to Live
TWILIO_ACCOUNT_SID=AC_LIVE_TWILIO_SID
TWILIO_AUTH_TOKEN=LIVE_AUTH_TOKEN
TWILIO_FROM_NUMBER=+14155238886 (Approved WhatsApp Number)
```

---

## 📈 6. Future Expansion Blueprint (भविष्य का क्लाउड स्केलिंग रोडमैप)

कस्टमर द्वारा अपलोड की जाने वाली इमेजेस (Review Uploads) को हमेशा के लिए सुरक्षित रखने के दो रास्ते हैं:

### Option A: Render Persistent Volumes (No Code Change)
यदि आप बैकएंड कोड में कोई बदलाव नहीं करना चाहते:
1. Render डैशबोर्ड पर अपनी Web Service के **Disks** सेक्शन में जाएं।
2. **Add Disk** पर क्लिक करें।
3. **Mount Path** को `/app/uploads` पर सेट करें।
4. **फायदा:** अब कस्टमर के द्वारा अपलोड की गई तस्वीरें सर्वर रीस्टार्ट होने पर भी सुरक्षित रहेंगी।

### Option B: Cloudinary Migration Plan (Free Tier - Recommended)
जब स्टोर बड़ा हो जाए, तो इमेजेस को **Cloudinary Cloud** पर होस्ट करना सबसे बेस्ट है:

#### 1. Maven Dependency जोड़ना (`pom.xml`):
```xml
<dependency>
    <groupId>com.cloudinary</groupId>
    <artifactId>cloudinary-http44</artifactId>
    <version>1.36.0</version>
</dependency>
```

#### 2. Java Controller में बदलाव ([CustomerFeedbackController.java](file:///d:/MadhurGram/product-service/src/main/java/com/madhurgram/productservice/feedback/controller/CustomerFeedbackController.java)):
```java
// Cloudinary config initialize करना
Cloudinary cloudinary = new Cloudinary(ObjectUtils.asMap(
  "cloud_name", "your_cloud_name",
  "api_key", "your_api_key",
  "api_secret", "your_api_secret"
));

// लोकल फाइल राइट करने की जगह सीधे क्लाउड पर अपलोड करें:
@PostMapping("/public/feedback/upload")
public ResponseEntity<?> uploadFeedbackImage(@RequestParam("file") MultipartFile file) {
    try {
        // Direct upload to Cloudinary CDN
        Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.emptyMap());
        String fileUrl = (String) uploadResult.get("secure_url"); // CDN Link
        
        return ResponseEntity.ok(Map.of("url", fileUrl));
    } catch (Exception e) {
        return ResponseEntity.status(500).body("Upload failed");
    }
}
```
**फायदा:** इससे इमेज का लोड सीधे Cloudinary पर चला जाता है, जिससे आपका बैकएंड सर्वर कभी स्लो नहीं होता!

---

## 🎯 Solution Architect's Golden Advice:
घबराएं नहीं, शुरुआत हमेशा **फ्री टियर (Staging Env)** से करें। जैसे-जैसे आपके कस्टमर्स और सेल्स बढ़ेंगे, हम धीरे-धीरे (बिना किसी रुकावट के) डेटाबेस और सर्वर कैपेसिटी को अपग्रेड करते जाएंगे।
