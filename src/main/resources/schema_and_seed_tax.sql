-- =====================================================================
-- MADHURGRAM DATABASE MIGRATION & SEEDING SCRIPT
-- FEATURE: Indian GST Dynamic Tax Engine (CGST/SGST/IGST + HSN Codes)
-- =====================================================================

USE madhurgram_db;

-- 1. Create hsn_tax_master table to manage standard tax slabs
CREATE TABLE IF NOT EXISTS hsn_tax_master (
    hsn_code VARCHAR(10) PRIMARY KEY,
    description VARCHAR(150) NOT NULL,
    gst_rate DECIMAL(5,2) NOT NULL
);

-- 2. Add HSN Code reference to products table
ALTER TABLE products ADD COLUMN IF NOT EXISTS hsn_code VARCHAR(10);
ALTER TABLE products ADD CONSTRAINT fk_products_hsn_code 
    FOREIGN KEY IF NOT EXISTS (hsn_code) REFERENCES hsn_tax_master(hsn_code);

-- 3. Add tax columns to orders table to persist checkout tax snapshots
ALTER TABLE orders ADD COLUMN IF NOT EXISTS taxable_amount DECIMAL(12,2);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS cgst_total DECIMAL(12,2);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS sgst_total DECIMAL(12,2);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS igst_total DECIMAL(12,2);

-- 4. Add tax columns to order_items table to persist itemized tax snapshots
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS hsn_code VARCHAR(10);
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS gst_rate DECIMAL(5,2);
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS taxable_amount DECIMAL(12,2);
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS cgst_amount DECIMAL(12,2);
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS sgst_amount DECIMAL(12,2);
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS igst_amount DECIMAL(12,2);

-- 5. Seed default HSN tax rates for Indian retail compliance
INSERT IGNORE INTO hsn_tax_master (hsn_code, description, gst_rate) VALUES
('0405', 'Animal Fats & Ghee (dairy)', 12.00),          -- Ghee / Dairy (12% GST)
('1701', 'Cane Sugar & Jaggery (Gur)', 5.00),           -- Jaggery / Sweeteners (5% GST)
('1512', 'Vegetable & Mustard Oils', 5.00),             -- Mustard Oil / Fats (5% GST)
('2001', 'Pickles (preserved with oil/salt)', 12.00);   -- Pickles / Mango Achar (12% GST);

-- 6. Link existing default products in database to HSN codes by category
UPDATE products SET hsn_code = '0405' WHERE category = 'dairy';
UPDATE products SET hsn_code = '1701' WHERE category = 'sweeteners';
UPDATE products SET hsn_code = '1512' WHERE category = 'oils';
UPDATE products SET hsn_code = '2001' WHERE category = 'pickles';

-- 7. Add sales count columns for social proof features
ALTER TABLE products ADD COLUMN IF NOT EXISTS show_sales_count BOOLEAN DEFAULT FALSE;
ALTER TABLE products ADD COLUMN IF NOT EXISTS sales_count INT DEFAULT 0;

-- 8. Seed default values for existing products to prevent null mapping issues
UPDATE products SET show_sales_count = FALSE WHERE show_sales_count IS NULL;
UPDATE products SET sales_count = 0 WHERE sales_count IS NULL;
