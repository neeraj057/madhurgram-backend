# Immediate Staging Setup Checklist (स्टेजिंग तैयारी चेकलिस्ट)

इस चेकलिस्ट का उपयोग करके आप एक-एक करके सभी आवश्यक काम पूरे कर सकते हैं। जैसे ही कोई काम पूरा हो जाए, आप उसके आगे `[ ]` को `[x]` में बदल सकते हैं।

---

## 📂 1. Save & Push Code (कोड को गिट पर डालें)
* [ ] टर्मिनल/कमांड प्रॉम्प्ट खोलें और `d:\MadhurGram\product-service` और `c:\Users\victus\madhurgram-frontend` में जाकर निम्नलिखित गिट कमांड चलाएं:
  ```bash
  git status
  git add .
  git commit -m "chore: configuration fixes and load testing setup"
  git push origin main
  ```

---

## 🔒 2. Account Signups (फ्री अकाउंट बनाएं)
* [ ] **Vercel Accounts:**
  * [Vercel.com](https://vercel.com) पर जाएं।
  * **Continue with GitHub** चुनें ताकि आपके सारे रिपोजिटरी वहां दिख सकें।
* [ ] **Render Accounts:**
  * [Render.com](https://render.com) पर जाएं।
  * **GitHub** के साथ साइन-अप करें।
* [ ] **Upstash Accounts (Redis):**
  * [Upstash.com](https://upstash.com) पर जाएं।
  * GitHub से लॉग-इन करें और एक नया **Redis Database (Free tier)** बनाएं।

---

## 💾 3. Database Credentials (डेटाबेस की चाबियां)
* [ ] **Render MySQL Database बनाएं:**
  * Render डैशबोर्ड में **New +** -> **PostgreSQL / MySQL** चुनें।
  * Database Name: `madhurgram_db`
  * Username: (Render द्वारा जनरेट किया हुआ)
  * Password: (Render द्वारा जनरेट किया हुआ)
  * **External Connection String Copy करें:** (ये MySQL Workbench/DBeaver से कनेक्ट करने के काम आएगी)
* [ ] **Redis Connection Strings Copy करें:**
  * Upstash कंसोल से `Endpoint` (Host) और `Port` कॉपी करके रख लें।

---

## 🔑 4. Prepare Staging Variables (वैल्यूज तैयार रखें)
नीचे दिए गए वेरिएबल्स की वैल्यूज पहले से लिख लें ताकि डिप्लॉय करते समय सिर्फ कॉपी-पेस्ट करना पड़े:

* **SPRING_DATASOURCE_URL:** `jdbc:mysql://[your-render-db-host]:3306/madhurgram_db`
* **SPRING_DATASOURCE_USERNAME:** `[copy from render]`
* **SPRING_DATASOURCE_PASSWORD:** `[copy from render]`
* **REDIS_HOST:** `[copy host address from upstash]`
* **REDIS_PORT:** `[copy port number from upstash]`
* **JWT_SECRET:** `MadhurGramStagingSecureKey2026!#`
* **ADMIN_USERNAME:** `admin`
* **ADMIN_PASSWORD:** `StagingAdmin@2026`
