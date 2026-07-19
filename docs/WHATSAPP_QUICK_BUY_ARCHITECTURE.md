# WhatsApp "Quick Buy" Checkout & Order Channel Architecture

This document outlines the technical details and flow of **Feature 1: WhatsApp "Quick Buy" Checkout & Order Channel** implemented across the MadhurGram platform.

---

## 📞 1. Feature Description

Indian consumers heavily rely on WhatsApp for customer support and purchases. Many buyers find it tedious to navigate traditional multi-step checkout processes. 

The **WhatsApp Quick Buy** feature allows customers to instantly bypass the cart checkout flow by clicking an "Order on WhatsApp" button. This redirects them to a pre-filled WhatsApp message containing the product name and selected volume, sent directly to MadhurGram's business phone number. 

To give administrators total control over this sales channel, the feature is fully managed via a central control panel in the Admin Dashboard.

---

## 🗄️ 2. Database & REST API (Backend)

The backend saves configurations dynamically in the database to prevent hardcoding numbers or text templates.

### A. Database Storage
We store the settings in the existing `system_settings` table (managed by `SystemSetting` entity):
1. `WHATSAPP_QUICK_BUY_ENABLED`: `"true"` or `"false"`. Toggles storefront visibility.
2. `WHATSAPP_QUICK_BUY_NUMBER`: e.g. `"917899999902"`. Target phone number with country code (no `+` sign).
3. `WHATSAPP_QUICK_BUY_TEXT_TEMPLATE`: Customizable text template (e.g. `"नमस्ते MadhurGram, मुझे *{productName}* ({volume}) आर्डर करना है। मेरा नाम: ..., फोन: ..., पता: ...।"`).

### B. Controller Endpoints (`WhatsAppSettingsController.java`)
- `GET /api/public/settings/whatsapp`: Fetches public configuration parameters. Cached in client browser memory if needed.
- `GET /api/admin/settings/whatsapp`: Secured admin endpoint to fetch configs.
- `PUT /api/admin/settings/whatsapp`: Receives the updated config payload, updates the database, and registers an admin audit trail action (`UPDATE_WHATSAPP_SETTINGS`).

---

## 💻 3. Frontend Next.js Implementation

The frontend handles two views: the Admin Marketing dashboard page (managing settings) and the Product Quick View Modal (where customers interact).

### A. Admin Dashboard Control Panel (`marketing/page.tsx`)
Admins can toggle the feature, change the phone number, and customize templates inside a premium green-themed settings card:

```tsx
<input 
  type="text" 
  value={whatsappNumber} 
  onChange={(e) => setWhatsappNumber(e.target.value)} 
/>
```
Clicking **"Save WhatsApp Configuration"** PUTs the payload to `/api/admin/settings/whatsapp`.

### B. Storefront Button Integration (`ProductQuickViewModal.tsx`)
On component mount, the modal checks `/api/public/settings/whatsapp`. If `whatsappEnabled` is true and the item is in stock, a green **"Order on WhatsApp"** button is rendered:

1. **Substitution Engine:**
   When clicked, the client replacements substitute `{productName}` and `{volume}` tags:
   ```typescript
   let message = whatsappTemplate;
   message = message.replace(/{productName}/g, product.name);
   message = message.replace(/{volume}/g, selectedVolume);
   ```
2. **Tab Redirection:**
   Redirects the browser using a safe external tab window open:
   ```typescript
   const waUrl = `https://wa.me/${whatsappNumber}?text=${encodeURIComponent(message)}`;
   window.open(waUrl, "_blank");
   ```
This provides a fluid transition from storefront browsing directly to chat orders.
