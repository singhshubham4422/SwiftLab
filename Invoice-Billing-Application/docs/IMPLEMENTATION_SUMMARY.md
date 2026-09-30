# SwiftLab Inventory & Billing Platform - Implementation Summary

## 1. Executive Overview

This takeover implementation successfully migrated and expanded the **SwiftLab Invoice Billing Application** into the **SwiftLab Inventory & Billing Platform**. The platform is an offline-capable, local-first system with optional multi-device cloud synchronization, robust multi-tenant organization isolation, movement-based inventory integrity, and enterprise security.

All legacy features (invoices, PDF/Excel export, Excel import, company settings, authentication) have been preserved and upgraded to the modern architecture.

---

## 2. Completed Features

### 2.1 Multi-Tenant Organization Isolation
- Every core entity is bounded by `organization_id`.
- Tenant boundary enforcement at Service, Controller, and Database (Supabase Row Level Security) levels.
- Direct object ID tampering (IDOR) attacks are strictly prevented.

### 2.2 Security & Authentication
- Replaced insecure raw SHA-256 password storage with `BCryptPasswordEncoder` (strength 10).
- Transparent auto-upgrade of legacy SHA-256 hashes upon successful user login.
- Stateless REST Bearer Token authentication via `ApiTokenFilter` for external API and mobile clients.
- Role-Based Access Control (RBAC): `OWNER`, `ADMIN`, `MANAGER`, `STAFF`.
- OpenAPI 3.0 (Swagger UI) documentation at `/swagger-ui.html`.

### 2.3 Movement-Based Inventory Engine
- Auditable stock ledger: stock is never modified via arbitrary number replacement.
- Full movement lifecycle: `STOCK_IN`, `STOCK_OUT`, `SALE`, `PURCHASE`, `RETURN`, `ADJUSTMENT`, `TRANSFER`.
- Pessimistic write locking on stock rows during transactions to guarantee concurrency safety.
- Strict negative inventory prevention unless explicitly enabled by organization policy.
- Multi-warehouse support with automatic default warehouse provisioning.

### 2.4 Sales & Point of Sale (POS)
- Complete POS checkout flow with real-time stock deduction.
- Atomic creation of `Sale`, `SaleItem`, `StockMovement`, `Invoice`, and `Payment`.
- Partial and full payment tracking with multiple tender types (Cash, UPI, Card, Bank Transfer, Credit).
- Automatic invoice number generation and receipt view.

### 2.5 Purchasing & Supplier Management
- Purchase order creation and line-item tracking.
- Formal "Receive Goods" replenishment workflow with automatic `PURCHASE` stock movement generation.
- Full supplier CRM with tenant isolation.

### 2.6 Dual Data Modes & Offline-First Guarantees
- Two explicit modes: `LOCAL_ONLY` and `CLOUD_SYNC`.
- **Absolute Local-First Guarantee:** In `LOCAL_ONLY` mode, internet availability **NEVER** silently or automatically uploads local records to the cloud.
- Safe mode switching requiring explicit user opt-in and confirmation modal.
- Persistent random UUID device identity (`WIN-<UUID>`, `ANDROID-<UUID>`) with no invasive hardware fingerprinting.

### 2.7 Synchronization Engine & Idempotency
- Offline transactions are committed to the local H2 file database and enqueued in `sync_queue`.
- Strict idempotency key checking (`applied_sync_keys`) prevents duplicate sales, invoices, or stock movements upon retry replays.
- Movement-based additive conflict resolution preserves all distributed offline sales.

### 2.8 Web UI & Reporting
- Responsive Thymeleaf dashboard displaying: revenue, purchases, inventory valuation, low-stock warnings, invoice statuses, data mode, and sync health.
- Dedicated management interfaces for Products, Inventory, Stock Movements, Customers, Suppliers, Purchases, Sales, Invoices, Payments, Reports, Devices, Sync, and Users.

---

## 3. Database Schema

- **Local Storage:** Embedded H2 file database (`./data/invoicedb.mv.db`), persistent across reboots.
- **Cloud Storage:** Supabase PostgreSQL with 25 tables, foreign key constraints, performance indexes, and Row Level Security.
- Files located in `/database/`:
  - `schema.sql`: Full DDL for all 25 tables.
  - `indexes.sql`: Performance and isolation indexes.
  - `seed.sql`: Initial seed data (default organization, owner user, warehouse, units, categories).
  - `sync_schema.sql`: Synchronization queue and idempotency tables.
  - `rls.sql`: Row Level Security policies.
  - `README.md`: Database setup instructions.

---

## 4. REST API Endpoints

- Auth: `POST /api/auth/login`, `POST /api/auth/register`, `GET /api/auth/profile`
- Organizations: `GET /api/organizations/current`, `PUT /api/organizations/current`
- Users: `GET /api/users`, `POST /api/users`
- Products: `GET /api/products`, `POST /api/products`, `PUT /api/products/{id}`, `DELETE /api/products/{id}`
- Categories & Brands & Units: `/api/categories`, `/api/brands`, `/api/units`
- Warehouses: `GET /api/warehouses`, `POST /api/warehouses`
- Inventory: `GET /api/inventory`, `POST /api/inventory/adjust`, `GET /api/stock-movements`
- Customers & Suppliers: `GET/POST /api/customers`, `GET/POST /api/suppliers`
- Purchases: `GET/POST /api/purchases`, `POST /api/purchases/{id}/receive`
- Sales: `GET/POST /api/sales`
- Invoices: `GET /api/invoices`, `POST /api/invoices`, `GET /api/invoices/{id}/pdf`
- Payments: `GET /api/payments`, `POST /api/payments`
- Sync: `GET /api/sync/status`, `POST /api/sync/push`, `POST /api/sync/mode`
- Devices: `GET /api/devices`

---

## 5. Verification & Test Suite

Automated integration tests located in `src/test/java/com/example/invoiceapp/`:
1. `AuthAndSecurityTest`: BCrypt password encoding, legacy SHA-256 transparent upgrade, API token lifecycle.
2. `OrganizationIsolationTest`: Multi-tenant boundary checks across products, customers, suppliers, and invoices.
3. `ProductCatalogTest`: Product CRUD, SKU uniqueness per org, soft delete filtering.
4. `InventoryEngineTest`: Movement-based inventory lifecycle (+100 -5 +20 +2 -1 = 116 units), pessimistic locking, and negative stock guard.
5. `PurchaseWorkflowTest`: Purchase order creation, goods receipt, and automated inventory replenishment.
6. `SaleWorkflowTest`: POS checkout, stock check, stock deduction, automatic invoice creation, payment settlement.
7. `InvoiceAndPaymentTest`: Subtotal/tax/discount calculations, partial payments, status transitions, and PDF generation.
8. `SyncAndLocalModeTest`: Local-only mode blocks sync, mode switch requires explicit confirmation, idempotency key duplicate detection.

**Test Run Result:**
`Tests run: 10, Failures: 0, Errors: 0, Skipped: 0` - **BUILD SUCCESS**.

---

## 6. Windows Desktop Packaging

- `run.bat`: Launches the background Java process without console and opens browser to `http://localhost:8080/`.
- `package.bat`: Uses Maven and JDK 21 `jpackage` with WiX Toolset v3.11.
- Output: Standalone portable executable (`dist\InvoiceApp\InvoiceApp.exe`) and single-file Windows installer (`InvoiceApp-Installer.exe`).

---

## 7. Cloud Deployment (Render & Supabase)

- Multi-stage `Dockerfile` (Java 21 Alpine runtime) with unprivileged user execution.
- Blueprint `render.yaml` for automatic cloud deployment.
- Environment-variable-driven configuration (`SPRING_PROFILES_ACTIVE=cloud`, datasource credentials, Supabase keys).

---

## 8. Android Client Readiness

- Native Kotlin + Jetpack Compose architectural roadmap documented in `/docs/ANDROID_CLIENT_PLAN.md`.
- No hybrid / Flutter frameworks; consumes identical business logic via Spring Boot REST APIs with Bearer token authentication.
