# Advanced Live Orders Pipeline Dashboard (ऑर्डर्स पाइपलाइन गाइड)

Bhai, is document me dynamic pipeline tabs, realtime search filters, order status progress stepper trackers, executive stats metric cards, aur map geocoding shortcut integration ki details aur testing steps diye gaye hain.

---

## 🌟 Feature Overview

Admin users (जैसे मिठाई पैकर, डिलीवरी बॉय, और स्टोर मैनेजर) के काम को आसान बनाने और ऑर्डर्स को छूटने से बचाने के लिए, हमने लाइव ऑर्डर्स कंसोल को एक **हाई-फ़िडेलिटी पाइपलाइन डैशबोर्ड** में बदल दिया है:

1. **Executive Pipeline Stats (एक्जीक्यूटिव समरी):** पेज के सबसे ऊपर 6 लाइव मैट्रिक्स कार्ड हैं जो आज के ऑर्डर्स, कल के ऑर्डर्स, कुल पेंडिंग ऑर्डर्स, और डिलीवर हो चुके ऑर्डर्स की तुरंत संख्या बताते हैं।
2. **Search Indexing (सर्च बॉक्स):** ग्राहक का नाम, मोबाइल नंबर, ऑर्डर आईडी, पिनकोड, शहर या एड्रेस टाइप करके ऑर्डर्स को चुटकी में फ़िल्टर किया जा सकता है।
3. **Smart Pipeline Tabs (ऑर्डर कैटगरीज़):**
   * **Active Pipeline:** पेंडिंग, कन्फ़र्म्ड, शिप्ड, और आउट-फ़ॉर-डिलीवरी वाले सक्रिय ऑर्डर्स (जो अभी प्रोसेस होने हैं)।
   * **Pending Only:** केवल वे नए ऑर्डर्स जिन्हें एडमिन को कन्फ़र्म करना है।
   * **Today:** आज आए हुए सभी ऑर्डर्स।
   * **Yesterday:** कल आए हुए ऑर्डर्स।
   * **Delivered & Cancelled:** डिलीवर हो चुके और कैंसल किए गए पुराने ऑर्डर्स का इतिहास।
   * **All Logs:** पूरे ऑर्डर्स की कम्प्लीट लिस्ट।
4. **Interactive Progress Stepper (ऑर्डर टाइमलाइन):** प्रत्येक आर्डर कार्ड के अंदर एक विज़ुअल प्रोग्रेस बार है:
   `Pending` ➔ `Confirmed` ➔ `Shipped` ➔ `Out for Delivery` ➔ `Delivered`
   यह एडमिन को एक नज़र में बताता है कि आर्डर किस स्टेज पर है।
5. **Google Maps Shortcut Pin:** यदि आर्डर में डिलीवरी कोऑर्डिनेट्स (Latitude / Longitude) उपलब्ध हैं, तो कार्ड पर एक **"Map Coordinates"** बटन दिखता है। इस पर क्लिक करते ही सीधे डिलीवरी बॉय के फ़ोन में गूगल मैप्स पर सटीक लोकेशन खुल जाएगी!

---

## 🚀 Step-by-Step Testing Guide (टेस्ट कैसे करें)

### Scenario A: Stats Grid Verification (मैट्रिक्स चेक)
1. **Admin Console** खोलें और बाएं मेनू से **Live Orders** पर क्लिक करें।
2. **Expected Result:**
   * पेज के सबसे ऊपर 6 अलग-अलग डिब्बों (cards) में कुल ऑर्डर्स और आज के ऑर्डर्स की संख्या रीयल-टाइम में दिखेगी।

---

### Scenario B: Dynamic Tabs Switching (कैटगरीज़ बदलना)
1. अलग-अलग टैब्स पर क्लिक करें (जैसे: *Pending Only*, *Today*, *Yesterday*, *All Logs*)।
2. **Expected Result:**
   * ऑर्डर्स की लिस्ट तुरंत बदल जाएगी।
   * *Active Pipeline* में केवल वही ऑर्डर्स दिखेंगे जो अभी पेंडिंग या शिपिंग में हैं। पूरे इतिहास (*Completed & Cancelled*) के ऑर्डर्स वहाँ मिक्स होकर एडमिन को कन्फ्यूज़ नहीं करेंगे।

---

### Scenario C: Search Filter Verification (सर्च बार)
1. सर्च बॉक्स में टाइप करें: कोई पिनकोड (जैसे *"452002"*), कोई शहर (जैसे *"Indore"*), या ग्राहक का फोन नंबर।
2. **Expected Result:**
   * टाइप करते ही नीचे केवल वही ऑर्डर्स दिखेंगे जो सर्च क्वेरी से मैच होते हैं। यह सर्च बिना पेज रीलोड हुए रीयल-टाइम में चलती है।

---

### Scenario D: Google Maps Click (सटीक लोकेशन)
1. ऐसे ऑर्डर को देखें जिसे गूगल मैप्स ऑटो-कम्पलीट के जरिए कोऑर्डिनेट्स के साथ प्लेस किया गया था।
2. आर्डर आईडी के पास नीले रंग का **"Map Coordinates"** बटन दिखेगा।
3. उस पर क्लिक करें।
4. **Expected Result:**
   * एक नया ब्राउज़र टैब खुलेगा जिसमें सीधे गूगल मैप्स पर उस एड्रेस के कोऑर्डिनेट्स पिन्ड (Pinned) मिलेंगे।
