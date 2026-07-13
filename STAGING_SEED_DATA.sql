-- =====================================================================
-- MADHURGRAM STAGING DATABASE SEED DATA SCRIPT
-- RUN DIRECTLY ON AIVEN MYSQL (defaultdb)
-- =====================================================================

USE defaultdb;

-- ---------------------------------------------------------------------
-- 1. SEED TAX SLABS (HSN कोड दरें)
-- ---------------------------------------------------------------------
INSERT IGNORE INTO hsn_tax_master (hsn_code, description, gst_rate) VALUES
('0405', 'Animal Fats & Ghee (dairy)', 12.00),          -- Ghee / Dairy (12% GST)
('1701', 'Cane Sugar & Jaggery (Gur)', 5.00),           -- Jaggery / Sweeteners (5% GST)
('1512', 'Vegetable & Mustard Oils', 5.00),             -- Mustard Oil / Oils (5% GST)
('2001', 'Pickles (preserved with oil/salt)', 12.00);   -- Pickles / Mango Achar (12% GST)


-- ---------------------------------------------------------------------
-- 2. SEED PRODUCTS LIST (उत्पाद सूची)
-- ---------------------------------------------------------------------
INSERT INTO products (name, price, volume, image_url, tag, category, stock, is_active) VALUES
('MadhurGram Pure Shuddh Cow Ghee (A2)', 649.00, '500ml / 1L Ghee', '/images/cow_ghee.png', 'Bilona Method', 'dairy', 50, 1),
('MadhurGram Premium Bhais Ghee', 599.00, '500ml', '/images/bhais_ghee.png', 'Pure Traditional', 'dairy', 35, 1),
('MadhurGram Ayurvedic Jaggery (Gur)', 349.00, '1kg', '/images/jaggery.png', 'New', 'sweeteners', 100, 1),
('Wood-Pressed Mustard Oil (Kachchi Ghani)', 299.00, '1L', '/images/mustard_oil.png', 'Out of Stock', 'oils', 0, 1),
('MadhurGram Artisnal Mango Pickle', 449.00, '500g', '/images/mango_pickle.png', 'Kachchi Ghani', 'pickles', 20, 1),
('MadhurGram Pure Shuddh Cow Dahi (A2)', 200.00, '500ml / 1L Dahi', '/images/cow_dahi.png', 'Bilona Method', 'dairy', 50, 1),
('MadhurGram Jaggery Powder', 349.00, '500g', '/images/jaggery_powder.png', 'Organic Powders', 'sweeteners', 100, 1),
('MadhurGram Elaichi Gur', 399.00, '500g', '/images/elaichi_jaggery.png', 'Cardamom Infused', 'sweeteners', 100, 1),
('MadhurGram Til Gur', 399.00, '500g', '/images/til_jaggery.png', 'Sesame Covered', 'sweeteners', 100, 1),
('MadhurGram Badam Gur', 449.00, '500g', '/images/badam_jaggery.png', 'Almond Embedded', 'sweeteners', 100, 1),
('MadhurGram Ayurvedic Jaggery Jar', 399.00, '500g', '/images/jaggery_jar.png', 'Reusable Jar', 'sweeteners', 100, 1),
('MadhurGram Traditional Mixed Pickle', 449.00, '500g', '/images/mixed_pickle.png', 'Kachchi Ghani', 'pickles', 100, 1),
('MadhurGram Garlic Pickle', 449.00, '500g', '/images/garlic_pickle.png', 'Sun-Cured Barni', 'pickles', 100, 1),
('MadhurGram Green Chilli Pickle', 399.00, '400g', '/images/green_chilli_pickle.png', 'Spicy & Tangy', 'pickles', 100, 1);


-- ---------------------------------------------------------------------
-- 3. LINK PRODUCTS TO TAX MAPPINGS (कैटेगरी के अनुसार टैक्स जोड़ना)
-- ---------------------------------------------------------------------
SET SQL_SAFE_UPDATES = 0;

UPDATE products SET hsn_code = '0405' WHERE category = 'dairy';
UPDATE products SET hsn_code = '1701' WHERE category = 'sweeteners';
UPDATE products SET hsn_code = '1512' WHERE category = 'oils';
UPDATE products SET hsn_code = '2001' WHERE category = 'pickles';

SET SQL_SAFE_UPDATES = 1;


-- ---------------------------------------------------------------------
-- 4. SEED SYSTEM SETTINGS VALUES (सिस्टम सेटिंग्स)
-- ---------------------------------------------------------------------
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
