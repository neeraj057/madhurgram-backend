# MadhurGram Pagination Architecture & Implementation Guide

This document provides a comprehensive technical overview of the pagination architecture implemented across the MadhurGram backend (Spring Boot + Java 21) and frontend (Next.js 16 + TypeScript) applications.

---

## 🌾 1. Why Pagination? ( scalability & Performance )

Initially, the application retrieved entire datasets (e.g., all orders, customers, reviews, and returns) in a single database query. While acceptable for a tiny catalog, this creates critical bottlenecks at scale:
1. **JVM Memory exhaustion (OutOfMemoryError):** Loading thousands of rows into server RAM as Entity objects, mapping them to DTOs, and serializing them to JSON leads to high heap usage.
2. **High Database Load:** Full-table scans and fetch joins (such as `Order -> OrderItem`) lock resources and degrade read times.
3. **Poor Frontend FCP & SEO:** Fetching multi-megabyte JSON files over the network increases page load latency, negatively impacting SEO rankings and user conversions.

### Solutions Introduced
* **Server-side database-level pagination** using Spring Data JPA's `Pageable`.
* **Page-aware caching** utilizing dynamic keys in Redis.
* **Hybrid Backward-Compatible Endpoints** allowing incremental storefront upgrades.
* **Responsive, premium paginated UI** in the admin dashboard.

---

## 🗄️ 2. Backend Architecture Details

The backend follows a loose-coupled MVC architecture. We implemented pagination from the database query layer up to the REST controller.

### A. Repository Layer (SQL/JPA level)
We defined paginated query methods in the repository layer using Spring Data's `Pageable`. Under the hood, Hibernate translates this into MySQL `LIMIT` and `OFFSET` queries.

#### Examples:
1. **Order Repository:**
   ```java
   @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.orderItems")
   Page<Order> findAllWithItems(Pageable pageable);
   ```
2. **Feedback Repository:**
   ```java
   Page<CustomerFeedback> findAllByOrderByCreatedAtDesc(Pageable pageable);
   ```
3. **Product Repository:**
   ```java
   Page<Product> findByIsActiveTrue(Pageable pageable);
   ```

#### Optimized Two-Step Query for Customer CRM Stats:
To avoid loading all orders into memory to group stats (VIP status, total spent) per customer, we introduced an optimized two-step process:
1. Fetch a paginated list of distinct customer phone numbers:
   ```java
   @Query(value = "SELECT DISTINCT o.phone_number FROM orders o", 
          countQuery = "SELECT COUNT(DISTINCT o.phone_number) FROM orders o", 
          nativeQuery = true)
   Page<String> findDistinctPhoneNumbers(Pageable pageable);
   ```
2. Batch query all orders only for those specific phone numbers on the current page:
   ```java
   @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.orderItems WHERE o.phoneNumber IN :phoneNumbers")
   List<Order> findOrdersWithItemsByPhoneNumbers(@Param("phoneNumbers") List<String> phoneNumbers);
   ```
This scales easily even if there are millions of orders, as it only processes 10 customers' orders at a time.

---

### B. Controller Layer (Backward Compatibility)
To prevent immediate runtime errors on the Next.js frontend pages that were not yet updated, the REST controllers dynamically evaluate the query parameters:
* If query params `page` and `size` are **missing**: Returns a `List<T>` (JSON Array `[...]`).
* If query params `page` and `size` are **provided**: Returns a Spring `Page<T>` (JSON Object `{ content: [...], totalPages: X, totalElements: Y, ... }`).

#### Controller Signature Example (`OrderController.java`):
```java
@GetMapping
public ResponseEntity<?> getAllOrders(
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size) {
    
    if (page != null && size != null) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("orderDate").descending());
        Page<OrderResponseDTO> paginated = orderService.getAllOrders(pageable);
        // Data masking logic for non-super-admins...
        return ResponseEntity.ok(paginated);
    } else {
        List<OrderResponseDTO> dtos = orderService.getAllOrders();
        // Fallback backward-compatible list mapping...
        return ResponseEntity.ok(dtos);
    }
}
```

---

### C. Page-Aware Caching (Redis/Spring Cache)
Caching a paginated endpoint requires saving different pages under unique cache keys. If we just cached under `'active'`, page 1 and page 2 requests would fetch the same cached content.

We resolved this in `AdminProductServiceImpl.java` and `ProductServiceImpl.java` using Spring EL expressions:
```java
@Override
@Cacheable(value = "products", key = "'public_active_' + #pageable.pageNumber + '_' + #pageable.pageSize")
public Page<ProductDTO> getAllActiveProductsForPublic(Pageable pageable) {
    Page<Product> page = productRepository.findByIsActiveTrue(pageable);
    return page.map(productMapper::toProductDTO);
}
```
This stores the cache keys as `public_active_0_10`, `public_active_1_10`, etc., preventing page conflicts.

---

## 💻 3. Frontend Next.js Integration

The Next.js frontend fetches the paginated APIs, parses the response, and renders premium UI controls.

### A. Custom Hooks Integration
Frontend hooks parse the response payload dynamically. This ensures that whether the backend returns a list (array) or a page (object), the frontend handles it gracefully.

#### Example Hook Implementation (`useAdminOrders.ts`):
```typescript
const [orders, setOrders] = useState<Order[]>([]);
const [page, setPage] = useState(0);
const [totalPages, setTotalPages] = useState(1);

const fetchOrders = async (pageIndex = page) => {
  try {
    const data = await apiClient<any>(`/api/orders?page=${pageIndex}&size=10`);
    if (Array.isArray(data)) {
      setOrders(data);
      setTotalPages(1);
    } else {
      setOrders(data.content || []);
      setTotalPages(data.totalPages || 1);
      setPage(data.number || 0);
    }
  } catch (error) {
    console.error("Error fetching orders:", error);
  }
};
```

---

### B. UI Pagination Controls
Each admin dashboard view features an elegant pagination component at the bottom of the listings matching the dark/gold theme of MadhurGram:

```tsx
{!loading && totalPages > 1 && (
  <div className="flex items-center justify-between border-t border-gray-800/60 pt-6 mt-8">
    <button
      onClick={() => setPage(Math.max(0, page - 1))}
      disabled={page === 0}
      className="px-4 py-2 border border-gray-800 bg-[#161616] rounded-lg text-xs font-bold text-gray-400 hover:text-[#D4AF37] hover:border-[#D4AF37]/50 disabled:opacity-30 disabled:pointer-events-none transition-all cursor-pointer"
    >
      Previous
    </button>
    <span className="text-xs font-mono text-gray-400">
      Page <span className="text-[#D4AF37] font-bold">{page + 1}</span> of <span className="text-white font-bold">{totalPages}</span>
    </span>
    <button
      onClick={() => setPage(Math.min(totalPages - 1, page + 1))}
      disabled={page === totalPages - 1}
      className="px-4 py-2 border border-gray-800 bg-[#161616] rounded-lg text-xs font-bold text-gray-400 hover:text-[#D4AF37] hover:border-[#D4AF37]/50 disabled:opacity-30 disabled:pointer-events-none transition-all cursor-pointer"
    >
      Next
    </button>
  </div>
)}
```

---

## 🛠️ 4. API Reference for Testing

You can verify and interact with these paginated endpoints via standard tools (Postman, curl, or Swagger at `http://localhost:8080/swagger-ui.html`):

### 1. Admin Orders
* **Unpaginated:** `GET /api/orders`
* **Paginated:** `GET /api/orders?page=0&size=5`

### 2. Admin Customers CRM
* **Unpaginated:** `GET /api/admin/customers`
* **Paginated:** `GET /api/admin/customers?page=0&size=10`
* **Paginated with Search:** `GET /api/admin/customers?search=99999&page=0&size=10`

### 3. Customer Feedbacks
* **Unpaginated:** `GET /api/admin/feedback`
* **Paginated:** `GET /api/admin/feedback?page=0&size=10`

### 4. Return Requests
* **Unpaginated:** `GET /api/returns/admin/all`
* **Paginated:** `GET /api/returns/admin/all?page=0&size=5`

### 5. Public Product Catalog
* **Unpaginated:** `GET /api/products?category=shop-all`
* **Paginated:** `GET /api/products?category=dairy&page=0&size=4`
* **Public Route Paginated:** `GET /api/public/products?page=0&size=8`
