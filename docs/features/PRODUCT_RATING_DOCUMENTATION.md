# Product Rating System - Architecture & Management Guide

We have successfully integrated a **database-driven product rating system** for MadhurGram. This document provides a complete breakdown of how the feature is implemented, how it maps across layers, and how to manage ratings as an administrator.

---

## 📐 Architecture Overview

The rating system uses a **curated database-driven approach** (Option B). This allows administrators to set realistic ratings for each product directly from the backend catalog, which are automatically reflected in real-time on the storefront.

```
+------------------+     Hibernate     +-----------------+
|  MySQL Database  |  =============>   | Backend Entity  |
|  (products table)|                   | (Product.java)  |
+------------------+                   +--------+--------+
                                                |
                                                | ProductMapper
                                                v
+------------------+     REST API      +--------+--------+
|  Frontend Client |  <=============   |   ProductDTO    |
| (ProductGrid.tsx)|                   |   (JSON Data)   |
+------------------+                   +-----------------+
```

---

## 🛠️ Code Implementations & Changes

### 1. Database & Backend Entity (`product-service`)

* **Product.java:** 
  Added a `rating` field mapped to a `decimal(3,2)` column.
  * Defaults to `4.8` for all existing and newly created products.
  ```java
  @Builder.Default
  @Column(name = "rating", precision = 3, scale = 2)
  private java.math.BigDecimal rating = java.math.BigDecimal.valueOf(4.8);
  ```

* **ProductDTO.java:**
  Exposed the rating field to the frontend JSON API payload.
  ```java
  private java.math.BigDecimal rating;
  ```

* **ProductMapper.java:**
  Maps the entity rating into the DTO response, providing a fallback default value.
  ```java
  .rating(product.getRating() != null ? product.getRating() : java.math.BigDecimal.valueOf(4.8))
  ```

* **AdminProductServiceImpl.java:**
  Allows administrators to specify and modify product ratings:
  * When adding a product (`addProduct`): maps the incoming rating.
  * When updating a product (`updateProduct`):
    ```java
    if (dto.getRating() != null) {
        product.setRating(dto.getRating());
    }
    ```

---

### 2. Frontend Client (`madhurgram-frontend`)

* **ProductGrid.tsx:**
  * **Product Interface:** Updated interface to register the `rating` attribute returned by the backend catalog API:
    ```typescript
    interface Product {
      ...
      rating?: number;
    }
    ```
  * **Product Card Placement:** Restored tag elements (`Bilona Method`, `Sold Out`, `15% OFF`) to their original top-right and top-left corners. Placed the star rating nicely alongside the volume info:
    ```tsx
    <div className="mt-1.5 flex items-center gap-1.5 select-none text-[11px] text-gray-500 font-medium">
      <span>{product.volume}</span>
      <span className="text-gray-300 text-[9px]">|</span>
      <div className="flex items-center gap-0.5 text-xs text-[#D4AF37]">
        <span className="text-sm leading-none">★</span>
        <span className="font-bold text-gray-800 text-[11px] font-mono leading-none">
          {product.rating ? Number(product.rating).toFixed(1) : "4.8"}
        </span>
      </div>
    </div>
    ```
  * **Quick View Modal:** Added the dynamic rating display under the product title:
    ```tsx
    <div className="flex items-center gap-1.5 mt-2 select-none">
      <div className="flex items-center gap-0.5 px-2 py-0.5 bg-yellow-500/10 rounded border border-yellow-500/25 text-[#D4AF37] text-xs font-bold font-mono">
        <span>★</span>
        <span>{selectedProduct.rating ? Number(selectedProduct.rating).toFixed(1) : "4.8"}</span>
      </div>
      <span className="text-[9px] text-gray-500 font-bold uppercase tracking-widest">
        Customer Choice
      </span>
    </div>
    ```

---

## ✍️ How to Manage Ratings (Admin Guide)

Since the database uses automatic schema updates (`spring.jpa.hibernate.ddl-auto=update`), the `rating` column is automatically added on application startup.

### Modifying Ratings via SQL
You can modify ratings directly in your MySQL Database (using MySQL Workbench or any SQL client):

1. **Update a specific product's rating:**
   ```sql
   UPDATE products 
   SET rating = 4.9 
   WHERE id = 1; -- e.g., A2 Cow Ghee
   ```

2. **Bulk set high ratings for new items:**
   ```sql
   UPDATE products 
   SET rating = 4.7 
   WHERE category = 'oils';
   ```

3. **Check current product ratings:**
   ```sql
   SELECT id, name, category, price, rating FROM products;
   ```
