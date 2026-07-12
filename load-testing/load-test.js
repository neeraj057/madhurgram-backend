import http from 'k6/http';
import { check, sleep } from 'k6';

// -----------------------------------------------------------------------------
// MadhurGram Performance Load Test Setup (k6 Script)
// Run command: k6 run load-test.js
// -----------------------------------------------------------------------------

export const options = {
  // Ramp-up/down stages to simulate up to 500-1000 Virtual Users (VUs)
  stages: [
    { duration: '1m', target: 200 },  // Ramp up from 0 to 200 users over 1 minute
    { duration: '2m', target: 500 },  // Ramp up to 500 users and sustain for 2 minutes
    { duration: '2m', target: 1000 }, // Peak load: Ramp up to 1000 users for 2 minutes
    { duration: '1m', target: 0 },    // Cool down: Ramp down back to 0 users over 1 minute
  ],
  
  // Performance Quality Targets (SLA thresholds)
  thresholds: {
    http_req_failed: ['rate<0.01'],    // Error rate must be less than 1%
    http_req_duration: ['p(95)<300'],  // 95% of requests must complete under 300ms
  },
};

// Target environment configuration
const BASE_URL = __ENV.TARGET_URL || 'http://localhost:8080';

export default function () {
  // 1. User visits Homepage & Catalog
  // Hits the public product catalog (should be extremely fast, cached if possible)
  const catalogRes = http.get(`${BASE_URL}/api/products?category=shop-all`, {
    headers: { 'Content-Type': 'application/json' },
  });
  
  check(catalogRes, {
    'catalog status is 200': (r) => r.status === 200,
    'catalog load time < 200ms': (r) => r.timings.duration < 200,
  });

  sleep(1); // User spends 1 second looking at products

  // 2. User adds an item to their cart
  const cartPayload = JSON.stringify({
    phoneNumber: `9876${Math.floor(100000 + Math.random() * 900000)}`, // Random 10-digit format
    items: [
      { productId: 1, quantity: 2 }
    ]
  });

  const cartRes = http.post(`${BASE_URL}/api/cart/update`, cartPayload, {
    headers: { 'Content-Type': 'application/json' },
  });

  check(cartRes, {
    'cart update status is 200 or 429': (r) => r.status === 200 || r.status === 429,
  });

  sleep(2); // User waits 2 seconds before checkout

  // 3. User places an order (COD or Prepaid simulator)
  const orderPayload = JSON.stringify({
    customerName: 'LoadTest User',
    phoneNumber: `9876${Math.floor(100000 + Math.random() * 900000)}`,
    address: '123 Test Street, Sarafa Bazar',
    pincode: '452002',
    cityState: 'Indore, Madhya Pradesh',
    latitude: 22.7196,
    longitude: 75.8577,
    paymentStatus: 'COD', // Cash On Delivery
    totalAmount: 250.00,
    orderItems: [
      {
        productId: 1,
        productName: 'Pure Desi Ghee',
        quantity: 1,
        price: 250.00
      }
    ]
  });

  const orderRes = http.post(`${BASE_URL}/api/orders/place`, orderPayload, {
    headers: { 'Content-Type': 'application/json' },
  });

  // Note: Since Order Place has a Rate Limiter (5 requests/min per IP),
  // we expect some 429 Too Many Requests status codes under high load.
  check(orderRes, {
    'order status is 200, 201 or 429': (r) => r.status === 200 || r.status === 201 || r.status === 429,
  });

  sleep(3); // Wait before next loop cycle
}
