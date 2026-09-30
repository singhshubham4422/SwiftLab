# SwiftLab Inventory & Billing Platform - Architecture Specification

## 1. System Overview

SwiftLab is an offline-capable, local-first inventory and billing platform built with Java 21 and Spring Boot 3. It serves multiple client form-factors:
1. **Web Application:** Responsive browser-based UI rendered via Thymeleaf and modern CSS.
2. **Windows Desktop (.exe):** Native, zero-configuration desktop client embedding an H2 file-based persistence engine.
3. **Android Client (Mobile):** Android-ready via stateless REST API endpoints with Bearer token authentication.

```
+-----------------------------------------------------------------------------------+
|                                  CLIENT LAYER                                     |
|  +--------------------+   +-----------------------+   +------------------------+  |
|  |   Web (Thymeleaf)  |   |  Windows (.exe + H2)  |   | Android (Jetpack/Room) |  |
|  +--------------------+   +-----------------------+   +------------------------+  |
+-----------------------------------------------------------------------------------+
                                       |
                                       v
+-----------------------------------------------------------------------------------+
|                        SPRING BOOT 3 (BUSINESS ENGINE)                            |
|  - Security & RBAC Filter Chain (BCrypt, Bearer ApiTokenFilter, Tenant Context)   |
|  - Data Mode Manager (LOCAL_ONLY vs CLOUD_SYNC)                                   |
|  - Movement-Based Inventory Ledger & Stock Engine (Pessimistic Locking)           |
|  - Integrated POS Sales, Purchasing, Invoicing, Payments, Audit Trail             |
|  - Idempotent Multi-Device Synchronization Pipeline                               |
+-----------------------------------------------------------------------------------+
             |                                                  |
             v                                                  v
+-----------------------------+               +-------------------------------------+
|    LOCAL STORAGE (H2 FILE)  |               |    CLOUD STORAGE (SUPABASE POSTGRES)|
| - Embedded file persistence |               | - Multi-tenant PostgreSQL 15+       |
| - Zero network requirement  |               | - Row Level Security (RLS)          |
| - Survives app/OS restarts  |               | - High availability & central sync  |
+-----------------------------+               +-------------------------------------+
```

---

## 2. Organization Multi-Tenancy Architecture

All operational entities within SwiftLab belong strictly to an `Organization`:
- `Organization`
  - `Users` (OWNER, ADMIN, MANAGER, STAFF)
  - `Products`, `Categories`, `Brands`, `Units`
  - `Warehouses`, `Stocks`, `StockMovements`
  - `Customers`, `Suppliers`
  - `Purchases`, `PurchaseItems`
  - `Sales`, `SaleItems`
  - `Invoices`, `InvoiceItems`, `Payments`
  - `Devices`, `SyncQueueItems`, `AuditLogs`

### Isolation Guarantees:
- **Service Layer Scoping:** Every query and write operation resolves the caller's `organizationId`. Queries without tenant scoping are strictly prohibited.
- **Controller Layer Security:** Any attempt by an authenticated user to access or manipulate an entity ID belonging to a different tenant results in an immediate `404 Not Found` or `403 Forbidden`.
- **Database Row Level Security (RLS):** Supabase PostgreSQL policies enforce `organization_id = current_org_id()`, preventing cross-tenant leakage at the database layer.

---

## 3. Security & Authentication Architecture

1. **Password Hashing:**
   - Standard: `BCryptPasswordEncoder` (strength 10).
   - Legacy Upgrade: Legacy SHA-256 passwords from previous versions are verified on successful login and automatically re-hashed to BCrypt transparently.
2. **Session & API Token Authentication:**
   - Web Browser: Standard HTTP Session with CSRF protection and role-based path authorization.
   - REST API / Mobile / Desktop Sync: Bearer Token authentication via `ApiTokenFilter` and `api_tokens` table.
3. **Role-Based Access Control (RBAC):**
   - `OWNER`: Full administrative access, tenant settings, user management, financial reports.
   - `ADMIN`: Tenant configuration, warehouse management, catalogs, purchases, sales, invoices.
   - `MANAGER`: Inventory adjustments, purchasing, sales processing, customer CRM.
   - `STAFF`: POS checkout, invoice creation, payment recording.

---

## 4. Movement-Based Inventory Architecture

Inventory in SwiftLab is auditable and ledger-based:
- Stock is never mutated via arbitrary number replacement.
- Every physical change produces a `StockMovement` row with:
  - Movement Type (`STOCK_IN`, `STOCK_OUT`, `SALE`, `PURCHASE`, `RETURN`, `ADJUSTMENT`, `TRANSFER`).
  - Quantity delta (positive or negative).
  - Exact `balanceAfter`.
  - Reference link (`referenceType`, `referenceId`).
  - Terminal identifier (`deviceId`) and user attribution (`createdBy`).
- **Concurrency & Atomicity:** Stock mutations employ pessimistic locking (`@Lock(LockModeType.PESSIMISTIC_WRITE)`) to eliminate race conditions during high-volume sales. Negative inventory is strictly blocked unless explicitly authorized.

---

## 5. Dual Data Mode Architecture

The system operates in two explicit data modes:
1. `LOCAL_ONLY` Mode:
   - Data resides strictly in the local H2 file database.
   - No external network calls, no telemetry, no Supabase sync.
   - **Crucial Invariant:** Internet connectivity restoration will NEVER trigger automated data uploads.
2. `CLOUD_SYNC` Mode:
   - Local transactions are committed immediately to the local database.
   - Operations are appended to `sync_queue` as `PENDING`.
   - Asynchronous sync worker flushes pending items to the cloud endpoint with retry backoff and idempotency validation.
   - Multi-device conflicts are resolved deterministically (inventory movements accumulated, master data versioned).
