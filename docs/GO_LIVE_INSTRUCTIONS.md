# MadhurGram Go-Live & Launch Checklist (लॉन्च गाइड)

Bhai, is document me simple Hinglish/WhatsApp language me likha hai ki site ko completely live (Production mode) karne ke liye aapke level par aur technical level par kya-kya pending hai, aur use kaise switch karna hai.

---

## 📅 Target Launch Date: 30 July / 1 August

---

## 📌 PART 1: Business Tasks (Aapke Level Par Jo Kaam Karne Hain)

### 1. Razorpay / Stripe Business KYC (Sabse Pehle Karein!)
* **Status:** PENDING
* **Kaam:** Razorpay ya Stripe par Business registration karein. Apne business ka GSTIN (agar hai), Pan Card, aur Bank Account details submit karein.
* **Kyu zaroori hai?** Gateway aapke documents verify karne me 3-5 din leta hai, isliye ise launch se kam-se-kam 15 din pehle shuru karein.

### 2. Product Catalog Cleanup (Real Assets)
* **Status:** PENDING
* **Kaam:** Abhi site par lagi saari dummy photos aur placeholder texts ko hata kar pure ghee, honey, aur traditional sweets ki asli professional photos upload karein.
* **Tip:** Photo shoot natural roop se village settings me karein, jisse MadhurGram ki authenticity ("Authentic Village Crafted") jhalak sake.

### 3. Server Hosting & Domain (Live URL)
* **Status:** PENDING
* **Kaam:** MadhurGram domain (`madhurgram.com`) purchase karein, aur frontend ko Vercel/Netlify par hosting ke liye connect karein. SSL certificate (HTTPS) lagana mandatory hai, kyu ki bina SSL ke live payment gateways kam nahi karenge.

---

## 🛠️ PART 2: Technical Tasks (Simulation se Live mode switch karne ke steps)

Jab aapka KYC aur Hosting ready ho jaye, to developer/tech level par in switches ko badalna hoga:

### 1. WhatsApp Notifications (Twilio Switch)
Abhi messages Console logs (Terminal) me print hote hain. Live karne ke liye:
1. **Twilio account verify karein:** Twilio dashboard par billing setup karke ek permanent WhatsApp Business Sender number purchase karein.
2. **Template registration:** WhatsApp ki policies ke anusar, aap jo Hindi templates (jaise `MG-000[ID] confirmed...`) bhej rahe hain, unhe Twilio/Meta dashboard par pre-approve karwana hoga.
3. **Properties Update:** Backend ki [application.properties](file:///d:/MadhurGram/product-service/src/main/resources/application.properties) me live credentials daal dein:
   ```properties
   twilio.account-sid=AC_REAL_LIVE_SID
   twilio.auth-token=REAL_AUTH_TOKEN
   twilio.from-number=+14155238886  # Aapka actual approved business number
   ```

### 2. Live Payment Gateway Switch (Prepaid Orders)
Abhi checkouts me card/UPI ka simulator popup dikhta hai. Live karne ke liye:
1. **Backend Configuration:** [application.properties](file:///d:/MadhurGram/product-service/src/main/resources/application.properties) me live Stripe API Keys daal dein:
   ```properties
   madhurgram.payment.provider=STRIPE # (Ya RAZORPAY)
   stripe.api.key=sk_live_YOUR_LIVE_KEY
   stripe.webhook.secret=whsec_YOUR_LIVE_WEBHOOK_SECRET
   ```
2. **Frontend Razorpay/Stripe SDK Link:**
   Frontend ke [CheckoutModal.tsx](file:///c:/Users/victus/madhurgram-frontend/src/components/features/checkout/CheckoutModal.tsx) me, hamare simulated popups (`showPaymentSimulator`) ko replace karke, standard SDK popup ko initiate karna hoga:
   ```javascript
   // Live SDK example (Razorpay)
   const options = {
     key: "rzp_live_your_live_key",
     amount: order.total * 100, // paise me conversion
     currency: "INR",
     name: "MadhurGram",
     description: "Pure handcrafted essentials",
     handler: function (response) {
       // Payment successful payment ID client send karega backend ko!
     }
   };
   const rzp = new window.Razorpay(options);
   rzp.open();
   ```

### 3. Courier & Logistics Switch (Delhivery / Shiprocket)
Abhi Delhivery/Shiprocket sandbox/test mode AWB tracks fake/mock generated return karte hain. Live karne ke liye:
1. **Logistics signup:** Delhivery One portal par login karke live merchant account activate karein.
2. **Live credentials:** Backend properties file me live integration token, live client name, aur pickup warehouse pincode ko update karein:
   ```properties
   delhivery.api.token=LIVE_PRODUCTION_TOKEN
   delhivery.pickup.location=VILLAGE_WAREHOUSE_NAME
   ```

---

## 📈 PART 3: Launch Strategy & Beta Testing (आखिरी 7 दिन)

1. **Internal Stress Testing:** Live jane se pehle payment settings ko "Test Mode" me hi rakh kar, apne 50 parivaar/dosto ko link bhejein aur test orders dalwayein. Verify karein ki:
   * SMS/WhatsApp alerts time par ja rahe hain.
   * Delivery slip auto-generate ho rahi hai.
   * Stock details sahi decrement ho rahe hain.
2. **Go-Live Switch:** Test successful hone ke baad, live payment settings activate karke site launch kar dein! 🚀
