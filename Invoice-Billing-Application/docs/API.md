# SwiftLab REST API Specification

## 1. Overview & OpenAPI Documentation

All REST APIs in SwiftLab return standard JSON, require proper HTTP verbs, and enforce strict tenant scoping.
- **Swagger UI Interactive Documentation:** `http://localhost:8080/swagger-ui.html`
- **OpenAPI 3.0 JSON Spec:** `http://localhost:8080/v3/api-docs`

---

## 2. Authentication & Authorization

All API endpoints except `/api/auth/login` and `/api/auth/register` require an HTTP header:
```http
Authorization: Bearer <API_TOKEN>
```

### 2.1 Login & Obtain Token
`POST /api/auth/login`
```json
{
  "username": "admin",
  "password": "your_password",
  "deviceId": "WIN-UUID-12345"
}
```
**Response (200 OK):**
```json
{
  "token": "eyJhbGci...",
  "username": "admin",
  "organizationId": 1,
  "role": "OWNER",
  "expiresAt": "2026-10-30T12:00:00Z"
}
```

---

## 3. Product & Catalog Endpoints

### 3.1 List Products
`GET /api/products?search=scanner&page=0&size=20`
**Response (200 OK):**
```json
{
  "content": [
    {
      "id": 10,
      "sku": "PRD-SCN-01",
      "name": "Barcode Scanner 2D",
      "costPrice": 45.00,
      "sellingPrice": 75.00,
      "currentStock": 35.0,
      "active": true
    }
  ],
  "totalElements": 1
}
```

### 3.2 Create Product
`POST /api/products`
```json
{
  "sku": "PRD-PRN-02",
  "name": "Thermal Receipt Printer",
  "categoryId": 1,
  "unitId": 1,
  "costPrice": 80.00,
  "sellingPrice": 120.00,
  "alertQuantity": 5.0
}
```

---

## 4. Movement-Based Inventory Endpoints

### 4.1 Get Stock Balances
`GET /api/inventory`
Returns real-time stock balances per warehouse and product.

### 4.2 Adjust Stock (Stock In / Stock Out / Adjustment)
`POST /api/inventory/adjust`
```json
{
  "warehouseId": 1,
  "productId": 10,
  "type": "STOCK_IN",
  "quantity": 25.0,
  "notes": "Incoming replenishment shipment"
}
```

### 4.3 Query Stock Movement Ledger
`GET /api/stock-movements?productId=10`
Returns timestamped movement ledger entries with before/after balances.

---

## 5. Sales & POS Checkout Endpoints

### 5.1 Execute Sale
`POST /api/sales`
```json
{
  "customerId": 1,
  "warehouseId": 1,
  "items": [
    {
      "productId": 10,
      "quantity": 2.0,
      "unitPrice": 75.00
    }
  ],
  "paymentStatus": "PAID",
  "paymentMethod": "CASH",
  "paidAmount": 150.00,
  "notes": "Store counter POS purchase"
}
```
**Response (201 Created):**
```json
{
  "id": 42,
  "saleNumber": "SALE-20260930-1001",
  "totalAmount": 150.00,
  "paidAmount": 150.00,
  "status": "COMPLETED",
  "invoiceId": 18
}
```

---

## 6. Purchase Orders & Goods Receipt

### 6.1 Create Purchase Order
`POST /api/purchases`
```json
{
  "supplierId": 1,
  "warehouseId": 1,
  "items": [
    {
      "productId": 10,
      "quantity": 50.0,
      "unitCost": 45.00
    }
  ],
  "notes": "Quarterly stock restock"
}
```

### 6.2 Receive Goods (Auto Stock-In)
`POST /api/purchases/{id}/receive`
Transitions purchase to `RECEIVED` and immediately triggers `PURCHASE` type stock movements for all line items.

---

## 7. Invoices & Payments

### 7.1 List Invoices
`GET /api/invoices?status=PENDING`

### 7.2 Download Invoice PDF
`GET /api/invoices/{id}/pdf`
Returns binary `application/pdf` generated via iText.

### 7.3 Record Payment
`POST /api/payments`
```json
{
  "invoiceId": 18,
  "amount": 50.00,
  "method": "UPI",
  "referenceNumber": "UPI-REF-88772",
  "notes": "Partial milestone settlement"
}
```

---

## 8. Multi-Device Synchronization Endpoints

### 8.1 Query Sync Status
`GET /api/sync/status`
```json
{
  "mode": "CLOUD_SYNC",
  "pendingCount": 0,
  "failedCount": 0,
  "lastSyncTime": "2026-09-30T13:40:00Z"
}
```

### 8.2 Push Client Queue Batch
`POST /api/sync/push`
```json
{
  "deviceId": "WIN-UUID-44321",
  "items": [
    {
      "id": 1,
      "entityType": "Sale",
      "entityId": 42,
      "operation": "CREATE",
      "payload": "{...}"
    }
  ]
}
```
Processes incoming sync items idempotently using `applied_sync_keys`.
