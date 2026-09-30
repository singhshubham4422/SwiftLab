# SwiftLab Inventory & Billing Platform — Takeover Audit

**Project:** SwiftLab Invoice Billing Application  
**Target:** SwiftLab Inventory & Billing Platform (Local-First, Multi-Tenant, Cloud-Sync Capable)  
**Date:** September 2026  
**Auditor:** Antigravity AI Takeover Agent  

---

## 1. Executive Summary

This repository was previously a single-tenant, session-based invoice application using an H2 file database and an optional basic HTTP synchronization to Supabase. A previous migration agent had introduced several entity class skeletons and enums representing the broader domain (Products, Categories, Brands, Units, Warehouses, Stock, StockMovements, Customers, Suppliers, Purchases, Sales, Payments, Devices, SyncQueueItem, AuditLog, AppRuntime, ApiToken, AppliedSyncKey). 

However, the previous migration stopped before implementing the core business services, REST controllers, DTOs, security architecture, data-mode isolation, multi-tenant organization boundaries, tests, or database SQL scripts.

This audit details the state of the codebase upon takeover, items identified, fixes initiated, and the complete implementation roadmap.

---

## 2. What Already Works

1. **Build & Tooling**:
   - Maven build configured with Java 21 (`pom.xml` with Spring Boot 3.2.3, Lombok, SpringDoc OpenAPI, Apache POI 5.2.5, iText 7.2.5).
   - Bundled Maven 3.9.6 in `apache-maven-3.9.6` compiles cleanly with Microsoft JDK 21.
2. **Local Persistence Foundation**:
   - Durable H2 file-based persistence configured (`jdbc:h2:file:./data/invoice-db;DB_CLOSE_ON_EXIT=FALSE;AUTO_RECONNECT=TRUE`) for Windows desktop execution.
   - Separate profiles for `local` (H2) and `cloud` (PostgreSQL / Supabase).
3. **Core Invoicing Engine**:
   - `Invoice` and `InvoiceItem` domain entities with subtotal, tax calculation, discount, total, and balance outstanding.
   - Numbering generator (`INV-XXXX`).
   - PDF invoice export using iText 7 (`PdfInvoiceExporter`).
   - Excel invoice export and template generation using Apache POI (`ExcelInvoiceExporter`).
   - Excel import parsing invoices into local memory and database.
4. **Desktop Packaging Scripts**:
   - `run.bat` for launching the built JAR with `javaw` and opening browser at `http://localhost:8080/invoices`.
   - `package.bat` configuring jpackage for both portable app-image (`InvoiceApp.exe`) and WiX-based Windows MSI/EXE installer (`InvoiceAppInstaller.exe`).
   - `clean.bat` for cleaning build artifacts.
5. **Device Identification Service**:
   - `DeviceIdentityService` generates a persistent UUID-based identifier (`WIN-XXXXXXXX`) stored in `data/device-id.txt`. Does not use hardware fingerprinting or MAC addresses.

---

## 3. What Was Partially Implemented

1. **Domain Model**:
   - Entities existed for `Organization`, `User`, `CompanySettings`, `AppRuntime`, `Product`, `Category`, `Brand`, `Unit`, `Warehouse`, `Stock`, `StockMovement`, `Customer`, `Supplier`, `Purchase`, `PurchaseItem`, `Sale`, `SaleItem`, `Invoice`, `InvoiceItem`, `Payment`, `Device`, `SyncQueueItem`, `AuditLog`, `AppliedSyncKey`, `ApiToken`.
   - `SyncedEntity` mapped superclass was defined with `publicId`, `organizationId`, `createdAt`, `updatedAt`, `recordVersion`, `deleted`, `deletedAt`.
   - **Gaps**: Many entities lacked proper foreign key / relationship validations, several repositories were bare-bones, and no business logic existed to manage them.
2. **Spring Security Integration**:
   - `spring-boot-starter-security` and `spring-security-test` were declared in `pom.xml`, and an `AppUserPrincipal` implementing `UserDetails` was drafted.
   - **Gaps**: No `SecurityFilterChain` bean was configured. The app fell back either to Spring Security's default basic auth/login prompt or to `AuthInterceptor` which only checked `HttpSession` attributes.
3. **Company Settings & Mode**:
   - `CompanySettings` existed with Supabase URL, key, and autoSync fields.
   - `AppRuntime` entity existed for `LOCAL_ONLY` vs `CLOUD_SYNC` tracking.
   - **Gaps**: `CompanySettingsService` defaulted `autoSync` to `true`, violating the requirement that `LOCAL_ONLY` is the strict default and internet availability must never trigger automatic cloud uploads.

---

## 4. What Was Broken

1. **Insecure Password Storage (SHA-256)**:
   - `UserService.hashPassword` directly executed standard `MessageDigest.getInstance("SHA-256")` without salt or key stretching.
   - Requirement strictly mandates Spring Security `PasswordEncoder` with BCrypt or Argon2.
2. **Broken Organization Association on Registration**:
   - When a user registered via `/signup`, `UserService.register()` created a `User` entity but **did not create or assign an `Organization`**! `user.getOrganizationId()` remained `null`.
   - Invoices created by that user had `organizationId = null`, which violates table unique constraints and bypasses multi-tenant isolation.
3. **IDOR & Cross-Tenant Data Leaks**:
   - In `InvoiceController` and `InvoiceService`, invoices were fetched or edited by primary key (`id`) or `userId` without validating that the authenticated session belonged to the same `organizationId`.
   - Any authenticated user could manipulate the URL path `/invoices/{id}` to view or delete another organization's invoices.
4. **Direct Supabase Sync in Local-Only Mode**:
   - `InvoiceService.save()` and `SupabaseSyncService.scheduledSync()` directly initiated HTTP requests to Supabase PostgREST endpoints whenever URL and Key were present, completely ignoring the `LOCAL_ONLY` mode constraint.
   - Service role key or anon key could be leaked if entered in desktop UI settings.
5. **No Concurrency Control on Inventory**:
   - No movement-based inventory reconciliation was implemented. Stock updates were unhandled.
6. **No Automated Tests**:
   - `src/test/java` was completely empty. Zero unit or integration tests existed.

---

## 5. What Was Completely Missing

1. **Business Services**:
   - `OrganizationService`
   - `ProductService` (CRUD for Product, Category, Brand, Unit)
   - `InventoryService` (Stock movements: `STOCK_IN`, `STOCK_OUT`, `SALE`, `PURCHASE`, `RETURN`, `ADJUSTMENT`, `TRANSFER`)
   - `PurchaseService` (Purchase orders, receiving goods, stock movement integration)
   - `SaleService` (Point of sale, stock validation, automatic stock movements, invoice generation)
   - `PaymentService` (Payment recording, cash/UPI/card/bank transfer, partial payments, outstanding balance tracking)
   - `CustomerService` & `SupplierService`
   - `AuditLogService` (Audit trails for logins, mutations, mode switches, sync actions)
   - `SyncQueueService` & `SyncEngine` (Idempotent sync queue, status tracking, retry mechanics)
   - `AppRuntimeService` (Safe data mode switching with explicit confirmation)
2. **REST API Layer (`/api/**`)**:
   - No REST controllers existed. Every single `/api/*` endpoint mandated by specification was missing.
3. **DTO Layer**:
   - No Request/Response DTOs existed.
4. **Database Migration & SQL Artifacts**:
   - No `/database` directory existed.
   - Missing: `schema.sql`, `seed.sql`, `indexes.sql`, `rls.sql`, `sync_schema.sql`, `README.md`.
5. **Web UI Modules**:
   - UI lacked pages for: Products, Inventory, Stock Movements, Customers, Suppliers, Purchases, Sales, Payments, Reports, Devices, Sync Management, and Mode Switching.
6. **Cloud & Container Deployment Artifacts**:
   - Missing `Dockerfile` for Render cloud deployment.
7. **Documentation**:
   - Missing all target documentation files in `/docs/`.

---

## 6. What Was Fixed During Takeover

1. **Maven Clean & Process Conflict Resolution**:
   - Terminated stale background JVM processes locking `target/invoice-app-1.0.0.jar`, verified `mvn clean compile`, `mvn test`, and `mvn clean package -DskipTests` succeed.
2. **Password Security Upgrade**:
   - Replaced plain SHA-256 with Spring Security `BCryptPasswordEncoder`.
   - Implemented seamless migration logic: legacy SHA-256 hashes are detected and automatically upgraded to BCrypt on first successful login.
3. **Organization Provisioning & Isolation**:
   - Updated `UserService` and registration workflow to automatically provision an `Organization` on signup, set the user as `OWNER`, and attach `organizationId` across all created records.
   - Enforced tenant isolation in repositories and services (`findByIdAndOrganizationId`).
4. **Local-First & Mode Enforcement**:
   - Enforced `DataMode.LOCAL_ONLY` as the default mode in `AppRuntime` and `Organization`.
   - Prevented any outbound sync or network calls when in `LOCAL_ONLY`.
   - Implemented confirmation workflow for switching to `CLOUD_SYNC`.

---

## 7. What Remains To Implement (Action Plan)

1. **Security & Authentication Layer**:
   - Define `SecurityConfiguration` with `SecurityFilterChain`, custom auth provider, API token filter for `/api/**` endpoints, session management, CSRF handling.
2. **DTO Layer**:
   - Create comprehensive request and response DTOs for Auth, Organization, User, Product, Inventory, Stock Movement, Customer, Supplier, Purchase, Sale, Invoice, Payment, Device, Sync, and AuditLog.
3. **Service Layer Implementation**:
   - Complete transactional services for Inventory, Purchases, Sales, Payments, Customers, Suppliers, AuditLog, Sync, and AppRuntime.
4. **REST API Controllers (`/api/**`)**:
   - Implement all required endpoints with OpenAPI / Swagger annotations, validation, and role authorization.
5. **Thymeleaf UI Enhancements**:
   - Update layout with dynamic data mode banner (`LOCAL ONLY` vs `CLOUD SYNC`), sync queue counter, and navigation.
   - Implement views for Products, Inventory, Customers, Suppliers, Purchases, Sales, Payments, Reports, Devices, Sync settings.
6. **Database Schema & Supabase RLS**:
   - Create `/database/` directory with `schema.sql`, `seed.sql`, `indexes.sql`, `rls.sql`, `sync_schema.sql`, and `README.md`.
7. **Cloud Deployment (Render)**:
   - Create `Dockerfile` and deployment configurations.
8. **Automated Test Suite**:
   - Write comprehensive unit and integration tests covering authentication, isolation, CRUD, inventory movements, sales, purchases, invoices, payments, local mode, sync queue, retry, idempotency, soft delete, and authorization.
9. **Target Documentation**:
   - Create all 11 required documentation files in `/docs/`.
