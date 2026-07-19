# MadhurGram — System Enhancements

## Features Completed
1. **WhatsApp "Quick Buy" Checkout & Order Channel**
2. **Pincode Delivery SLA & Courier Availability Checker**
3. **Expiry-based Batch Clearance (Dynamic Discounts)**
4. **Storytelling & Village Blog Hub (SEO)**

---

## 🕒 Feature 3: Expiry-based Batch Clearance

**Business Case:** Since MadhurGram sells natural, preservative-free village products, they have a limited shelf life. We needed a way to clear out batches nearing expiry to prevent inventory wastage.

### Backend Enhancements
- **Procurement Tracking:** Enhanced the `PurchaseOrder` entity in the database to track `batchNumber` and `expiryDate`.
- **Product Entity:** Added a `clearanceActive` flag to the `Product` entity.
- **Batch Expiry Scheduler:** Created `BatchExpiryScheduler` that runs automatically every day at 1:00 AM (`@Scheduled(cron = "0 0 1 * * ?")`).
  - **Logic:** It scans the database for `PurchaseOrder` batches where the `expiryDate` is within the next 45 days.
  - **Action:** If found, it does **NOT** automatically apply a discount (to protect profit margins on small batches). Instead, it logs an **Admin Alert** (`ADMIN ALERT: Product ID: ... has a batch nearing expiry...`). The Admin can then manually review the inventory and decide whether to toggle `clearanceActive` to put it on sale.

### Frontend Enhancements
- **Dynamic Badges:** Updated `ProductCard.tsx` to automatically display a pulsing orange `"⚡ Clearance"` badge instead of the standard black tag when a product is on clearance.
- **Urgency Banner:** Updated `ProductQuickViewModal.tsx` to display a highly visible alert banner above the product details: *"⚡ Clearance Flash Sale: Nearing expiry batch, grab it before it's gone!"*

### How to Test (Verification)
1. Set an `expiry_date` on a `purchase_orders` record to be within 30 days.
2. The scheduler will log an alert.
3. Once the admin manually sets `clearance_active = 1` in the `products` table, the frontend will instantly show the orange flash sale tags.

---

## 📖 Feature 4: Storytelling & Village Blog Hub

**Business Case:** Premium and pure village products sell based on *trust*. Customers want to know how the products are made, where they come from, and their health benefits. A blog section drives free SEO traffic from Google and converts readers into buyers.

### Backend Enhancements (New Module)
- **Entities & DB:** Created a new `Blog` entity with fields for `slug`, `title`, `author`, `content` (HTML), `imageUrl`, and `publishedAt`.
- **API Endpoints:** Added `BlogController.java` to expose:
  - `GET /api/public/blogs` (List of active blogs)
  - `GET /api/public/blogs/{slug}` (Single blog details)
- **Auto-Initialization:** Injected 2 premium dummy blogs via `@PostConstruct` into the database for immediate testing and UI rendering.

### Frontend Enhancements (Next.js SEO)
- **Server-Side Rendering (SSR):** Created `/app/blog/page.tsx` and `/app/blog/[slug]/page.tsx` to fetch data server-side. This ensures Google bots see the content immediately upon crawling.
- **Dynamic Meta Tags:** Implemented Next.js `generateMetadata()` in the individual blog page.
  - **Title:** Automatically sets the page title to `{Blog Title} | MadhurGram Stories`.
  - **Description:** Automatically extracts the first 160 characters of the blog's content (stripping HTML tags) to form the SEO meta description.
- **UI Design:** Created an elegant `BlogCard.tsx` component with hover animations and a premium, clean reading layout for the blog details page, featuring large cover images and typography optimized for long-form reading.

### How to Test (Verification)
1. Navigate to `http://localhost:3000/blog`.
2. You should see a beautifully designed grid containing the 2 auto-inserted dummy stories ("Bilona Ghee Kaise Banta Hai?" and "Gud (Jaggery) Khaane ke 5 Fayde").
3. Click on a story to read it. Open your browser's page source (Ctrl+U) to verify the `<title>` and `<meta name="description">` tags have been dynamically generated for SEO.
