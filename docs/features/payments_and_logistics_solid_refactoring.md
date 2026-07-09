# Feature Documentation: Payments & Logistics Refactoring with SOLID Principles

## 1. Overview
This documentation describes the modular, loosely coupled architecture implemented for MadhurGram's Payments & Logistics integrations. The system adheres to SOLID software design principles and leverages structural and behavioral patterns (Strategy, Factory, and Observer patterns) to deliver an extensible, production-ready backend engine.

---

## 2. Architectural Design Patterns Applied

### A. Observer Pattern (Spring ApplicationEvents)
- **Decoupled Workflow**: Instead of the order placement service (`OrderServiceImpl`) calling external shipping services directly, it publishes a domain event (`OrderConfirmedEvent`) once an order is marked as `CONFIRMED`.
- **Transaction and Thread Isolation**:
  - The event listener `OrderConfirmedListener` listens to this event.
  - `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` ensures that the pickup dispatch is only triggered *after* the database commits the transaction successfully.
  - `@Async` ensures that the pickup process runs on a separate thread pool (`task-executor-x`), allowing the main HTTP payment thread to immediately return a `200 OK` response to the payment gateway.

### B. Strategy and Factory Patterns (Open-Closed Principle)
- **Dynamic Adapters**: The payment and logistics components are defined behind abstract interfaces (`PaymentProcessor` and `LogisticsProvider`).
- **Dynamic Selection**: Strategy Factories (`PaymentStrategyFactory` and `LogisticsStrategyFactory`) load all available Spring beans implementing these interfaces at startup and resolve them at runtime via configuration properties or request payloads.
- **Provider Registry**:
  - Payments: `STRIPE` (`StripePaymentProcessor`), `RAZORPAY` (`RazorpayPaymentProcessor`).
  - Logistics: `DELHIVERY` (`DelhiveryLogisticsProvider`), `SHIPROCKET` (`ShiprocketLogisticsProvider`).

---

## 3. Class Definitions & Design Structure

### A. Event & Listener Layers
- **`OrderConfirmedEvent`**: Carries the confirmed order payload.
- **`OrderConfirmedListener`**: Captures events asynchronously post-commit and dispatches pickups via `LogisticsService`.

### B. Payment Abstractions
- **`PaymentProcessor` (Interface)**:
  - `String getProviderName()`
  - `String createPaymentSession(Order order)`
  - `boolean processWebhook(Map<String, Object> payload)`
- **`PaymentStrategyFactory`**: Autowires the list of processor strategies into a map matching strategy keys.
- **`PaymentController`**: Uses the factory to resolve the active provider strategy and processes events.

### C. Logistics Abstractions
- **`LogisticsProvider` (Interface)**:
  - `String getProviderName()`
  - `LogisticsShipmentResponse requestPickup(Order order)`
- **`LogisticsShipmentResponse` (Record)**: Immutable data containing `trackingNumber`, `courierName`, and `success`.
- **`LogisticsStrategyFactory`**: Instantiates and indexes carrier strategies.
- **`LogisticsService` (Interface)**: Segregated interface containing `void scheduleOrderPickup(Order order)`.
- **`LogisticsServiceImpl`**: Implementation resolving active provider configured in `application.properties`.

---

## 4. Verification Protocols

### A. Switch active providers via properties
You can switch gateways and shipping providers instantly by modifying configuration parameters in `application.properties`:
```properties
# Select active gateway: STRIPE or RAZORPAY
madhurgram.payment.provider=STRIPE

# Select active shipping carrier: DELHIVERY or SHIPROCKET
madhurgram.logistics.provider=DELHIVERY
```

### B. Trace Asynchronous Logging
Upon triggering a successful webhook event (`payment_intent.succeeded`):
1. **Thread 1 (`http-nio-8080-exec-X`)**:
   - Logs webhook verification.
   - Saves payment details as `COMPLETED`.
   - Publishes `OrderConfirmedEvent` and returns a HTTP `200 OK` immediately.
2. **Thread 2 (`task-executor-Y`)**:
   - Runs asynchronously after Thread 1 commits.
   - Logs: `OrderConfirmedEvent received for Order ID: X - dispatching asynchronous logistics pickup request`.
   - Fetches the active provider strategy (e.g. `Delhivery Express` or `Shiprocket Logistics`).
   - Generates the AWB code and updates status to `SHIPPED`.

---

## 5. Hinglish Dev Guide: Kaam Kaise Karta Hai, Testing, aur Future Extensibility

Bhai, is section me simple Hinglish/WhatsApp language me likha hai ki ye naya refactored SOLID system back-end me kaise kaam karta hai, isko test kaise karna hai, aur future me agar naye payment providers ya logistics partners add karne ho to bina core code ko touch kiye kaise karenge.

### A. Under the Hood Flow (Kaam Kaise Karta Hai?)
1. **Webhook Callback:**
   * Jab customer checkout process complete karta hai ya hum manual webhook trigger karte hain (`POST /api/payments/webhook`), tab control `PaymentController` ke paas aata hai.
   * `PaymentController` hamare config se active provider (Stripe ya Razorpay) check karta hai aur `PaymentStrategyFactory` se uska specific strategy processor fetch karke verification delegate kar deta hai.
2. **Order Confirmation & DB Commit:**
   * Agar payment verification pass ho jata hai, to controller order status ko `CONFIRMED` me update karta hai.
   * `OrderServiceImpl` order ko database me save karta hai. Jaise hi data database me save ho jata hai, ye Spring Event System ka use karke `OrderConfirmedEvent` publish karta hai.
3. **Transaction Commit & Async Trigger:**
   * Kyunki status change normal service thread `http-nio-8080-exec-*` pe chal raha hota hai, is transaction ke complete commit hone ke baad `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` trigger hota hai.
   * Listener check karta hai aur background task pool (`task-executor-*`) se ek naya thread assign karta hai, aur control waha shift ho jata hai.
   * `PaymentController` turant client ko custom response `200 OK` return kar dene me madad karta hai taaki checkout processing screen load hone me latency na ho.
4. **Logistics Dispatch (Background Thread):**
   * Background thread me chalte hue, listener `LogisticsService` ko invoke karta hai.
   * `LogisticsServiceImpl` app configuration se active logistics provider (Delhivery ya Shiprocket) read karta hai aur `LogisticsStrategyFactory` ke zariye active provider bean fetch karta hai.
   * Resolved provider pickup initiate karta hai, Waybill AWB generate karta hai, aur order status ko `SHIPPED` karke save kar deta hai, jisse dynamic tracking alert trigger ho jate hain.

---

### B. Testing Kaise Karen (Testing Guide)

Is modular system ko local machine pe test karne ke liye ye steps follow karein:

#### 1. Dynamic Settings Setup
`application.properties` me active providers config set karein:
```properties
# Toggle between STRIPE or RAZORPAY
madhurgram.payment.provider=STRIPE

# Toggle between DELHIVERY or SHIPROCKET
madhurgram.logistics.provider=DELHIVERY
```

#### 2. Triggering Webhook Success Simulation
Terminal me cURL command chala kar check karein ki webhook correctly process hota hai ya nahi (replace `<ID>` with actual order ID, e.g. `12`):
```bash
curl -X POST http://localhost:8080/api/payments/webhook \
  -H "Content-Type: application/json" \
  -d "{\"type\": \"payment_intent.succeeded\", \"data\": {\"orderId\": <ID>, \"transactionId\": \"ch_mock_stripe_99\", \"amount\": 1490.00}}"
```

#### 3. Log Verification Check
Apne Console logs check karein. Agar everything is perfect, aapko ye sequential flow dikhega:
1. `Received payment gateway webhook event: {...} via active provider: STRIPE` (Normal Execution thread)
2. `Publishing OrderConfirmedEvent for Order ID: <ID>`
3. `OrderConfirmedEvent received for Order ID: <ID> - dispatching asynchronous logistics pickup request` (Async Thread trigger!)
4. `Resolving logistics provider for: DELHIVERY`
5. `[DELHIVERY] Sending pickup request...`
6. `Pickup scheduled successfully via Delhivery Express. Waybill: AWB-DELHIVERY-XXXXXXXXXX`
7. `Order ID: <ID> status updated to SHIPPED in database`

#### 4. Switching Providers Test
Bina backend band kiye ya code badle, `application.properties` file me logictics provider change karein:
`madhurgram.logistics.provider=SHIPROCKET`
Aur dubara webhook run karein. Aap dekhenge ki logs me dynamic provider log badal jayega:
`Resolving logistics provider for: SHIPROCKET`
`[SHIPROCKET] Generating Shiprocket shipment order...`
`Pickup scheduled successfully via Shiprocket Logistics. Waybill: AWB-SHIPROCKET-XXXXXXXXXX`
Bina ek bhi line code badle system ne dusra logistics engine load kar liya!

---

### C. Future Me Change Kaise Karen? (Developer Extensibility Guide)

Bhai, agar future me system me kuch badalna ho to ye guidelines follow karein. Is pure design ko **Open-Closed Principle (OCP)** ke mutabik banaya gaya hai, isliye core code ko bina touch kiye updates ho jayenge.

#### Scenario 1: Naya Payment Gateway Add Karna Hai (e.g., Paytm ya Paypal)
1. Ek naya implementation class banayein jo `PaymentProcessor` interface ko implement kare:
   ```java
   @Service
   public class PaypalPaymentProcessor implements PaymentProcessor {
       @Override public String getProviderName() { return "PAYPAL"; }
       @Override public String createPaymentSession(Order order) { /* Paypal URL logic */ }
       @Override public boolean processWebhook(Map<String, Object> payload) { /* Signature check */ }
   }
   ```
2. Bas! Nayi class bante hi Spring Boot auto-discovery se ise strategies list me add kar dega.
3. Ab testing ya production ke liye, `application.properties` badlein:
   `madhurgram.payment.provider=PAYPAL`
   System khud naya strategy component run karega!

#### Scenario 2: Naya Courier Partner Add Karna Hai (e.g., Bluedart ya IndiaPost)
1. Ek class banayein jo `LogisticsProvider` interface ko implement kare:
   ```java
   @Component
   public class BluedartLogisticsProvider implements LogisticsProvider {
       @Override public String getProviderName() { return "BLUEDART"; }
       @Override public LogisticsShipmentResponse requestPickup(Order order) {
           // Bluedart API logic here
           return new LogisticsShipmentResponse("AWB-BLUEDART-1234", "Bluedart Delivery", true);
       }
   }
   ```
2. Ab properties badlein:
   `madhurgram.logistics.provider=BLUEDART`
   Automated listener bina kisi code change ke Bluedart engine trigger karega!

#### Scenario 3: Database Transaction ya Notification Actions Extend Karna
1. Agar order confirm hone par extra actions (jaise slack message, email confirmation, inventory ERP sync) add karni hon:
2. Hum `OrderServiceImpl` ko touch nahi karenge. Hum bas ek naya Listener class banayenge:
   ```java
   @Component
   public class OrderConfirmedSlackAlertListener {
       @Async
       @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
       public void notifySlack(OrderConfirmedEvent event) {
           // Slack alert logic here
       }
   }
   ```
3. Spring Framework automatic event propagation se naye listener ko call kar dega, jisse order workflow expand ho jayega with zero coupling and zero side effects!

#### Scenario 4: Frontend Se Testing (No Frontend Changes Required!)
Bhai, agar tum soch rahe ho ki is backend refactoring ke liye Frontend (Next.js) me koi changes karne padenge, to iska jawab hai: **Nahi, bilkul nahi!**
* **Kyu?** Kyunki humne SOLID design ke dynamic contracts follow kiye hain. Hamare APIs ke URLs aur request/response schemas same hain.
* **Testing Steps:**
  1. Frontend storefront dashboard pe jao, ek sample order place karo.
  2. Us order ke live tracking URL pe jao (e.g., `http://localhost:3000/orders/track/12`).
  3. Peeli color wali **"Retry Stripe/Razorpay Payment"** button pe click karo.
  4. Frontend automatically `/api/payments/webhook` pe trigger bhejega, aur backend background threads me pickup processing complete karke bina latency ke dynamic waybill allocate kar dega!


