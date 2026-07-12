-- =====================================================================
-- MADHURGRAM DATABASE SEEDING SCRIPT
-- FEATURE: Enterprise System Settings (WhatsApp Recovery & Review Suggestions)
-- =====================================================================

USE madhurgram_db;

INSERT IGNORE INTO system_settings (setting_key, setting_value, description) VALUES
('AUTO_RECOVERY_ENABLED', 'true', 'Enable/disable automated checkout recovery WhatsApp workflows'),
('WHATSAPP_RECOVERY_TEMPLATE', '{greeting} आपने MadhurGram पर शुद्ध, विलेज-क्राफ़्टेड Ghee & तेल्स कार्ट में छोड़े थे। 🌾\n\nऑर्डर पूरा करने के लिए आपकी कार्ट यहाँ सुरक्षित है। आपके लिए स्पेशल 5% डिस्काउंट कूपन तैयार है!\n\nComplete your order here: {deepLink}\n\nधन्यवाद, टीम MadhurGram 💛', 'WhatsApp template message for cart recovery reminders'),
('FEEDBACK_SUGGESTIONS_GHEE', 'Desi Ghee ka swad sach mein lajawab aur shuddh hai! 💛;Ghee ki dhoop jaisi khushboo ne dil jeet liya.', 'Suggested feedback comments for Ghee purchases'),
('FEEDBACK_SUGGESTIONS_OIL', 'Kachchi ghani tel ki shuddhata 100% genuine hai.;Tel ki packaging leak-proof aur secure thi.', 'Suggested feedback comments for Oil purchases'),
('FEEDBACK_SUGGESTIONS_HONEY', 'Pure honey ki mithas aur swad lajawab hai! 🍯', 'Suggested feedback comments for Honey purchases'),
('FEEDBACK_SUGGESTIONS_SWEETS', 'Mithai ka swad bilkul shuddh desi ghee jaisa hai!;Mithai bohot tazi aur naram thi.', 'Suggested feedback comments for Sweets purchases'),
('FEEDBACK_SUGGESTIONS_GENERIC', 'Delivery bilkul sahi samay par hui. 🚚;MadhurGram ke products ka swad bilkul gaanv jaisa authentic hai! ✨;Packaging bohot surakshit aur clean thi.;Customer support aur ordering experience bohot smooth tha.', 'Generic fallback review feedback comments suggestions'),
('ORDER_TEMPLATE_CONFIRMED', 'नमस्ते {customer}! आपका MadhurGram ऑर्डर (ID: {orderId}) कन्फर्म हो गया है। हम इसे जल्द ही पैक करके शिप करेंगे। धन्यवाद, टीम MadhurGram 💛', 'Template message for CONFIRMED order status notifications'),
('ORDER_TEMPLATE_SHIPPED', 'नमस्ते {customer}! आपका MadhurGram ऑर्डर (ID: {orderId}) शिप हो गया है। आपका ट्रैकिंग लिंक: {domain}/orders/track/{id} है। धन्यवाद, टीम MadhurGram 💛', 'Template message for SHIPPED order status notifications'),
('ORDER_TEMPLATE_OUT_FOR_DELIVERY', 'नमस्ते {customer}! आपका MadhurGram ऑर्डर (ID: {orderId}) आउट फॉर डिलीवरी है। हमारा डिलीवरी पार्टनर जल्द ही आपसे संपर्क करेगा। धन्यवाद, टीम MadhurGram 💛', 'Template message for OUT_FOR_DELIVERY order status notifications'),
('ORDER_TEMPLATE_DELIVERED', 'नमस्ते {customer}! आपका MadhurGram ऑर्डर (ID: {orderId}) सफलतापूर्वक डिलीवर हो गया है। हमें आशा है कि आपको हमारे शुद्ध उत्पाद पसंद आएंगे। कृपया अपना फीडबैक यहाँ दें: {domain}/feedback?orderId={id}। धन्यवाद, टीम MadhurGram 💛', 'Template message for DELIVERED order status notifications'),
('ORDER_TEMPLATE_CANCELLED', 'नमस्ते {customer}! आपका MadhurGram ऑर्डर (ID: {orderId}) कैंसिल कर दिया गया है। यदि आपने एडवांस पेमेंट किया था, तो रिफंड 3-5 दिनों में प्रोसेस हो जाएगा। धन्यवाद, टीम MadhurGram 💛', 'Template message for CANCELLED order status notifications');
