# SwiftLab Inventory & Billing Platform

**SwiftLab** is an enterprise-ready, offline-first Inventory, Point of Sale (POS), and Billing Management Platform. Designed for modern multi-device operations, it seamlessly balances offline autonomy with multi-tenant cloud synchronization across Web, Windows desktop workstations, and native Android devices.

---

## 1. Project Overview & Architecture

### High-Level Architecture
```
  +-------------------------------------------------------------------------+
  |                               Clients                                   |
  |  +---------------------+   +---------------------+   +---------------+  |
  |  |  Thymeleaf Web UI   |   |   Windows Desktop   |   |  Android App  |  |
  |  |  (SSR via Spring)   |   | (Offline H2 File DB)|   | (Room SQLite) |  |
  |  +----------+----------+   +----------+----------+   +-------+-------+  |
  +-------------|-------------------------|----------------------|----------+
                |                         |                      |
                | (Browser Session)       | (REST / Bearer Auth) | (REST / Bearer Auth)
                v                         v                      v
  +-------------------------------------------------------------------------+
  |              Render Cloud Web Service (Docker / Java 21)                |
  |                     Spring Boot 3 REST & MVC Engine                     |
  |    [Auth Filter]  [Tenant Scoping]  [Inventory Ledger]  [Sync Queue]    |
  +-------------------------------------------------------------------------+
                                      |
                                      | JDBC Over TLS (Pooler / 5432)
                                      v
  +-------------------------------------------------------------------------+
  |                        Supabase PostgreSQL Cluster                      |
  |       25 Relational Tables  •  Composite Indexes  •  RLS Tenant Guard    |
  +-------------------------------------------------------------------------+
```

### Key Pillars:
1. **Dual Operating Modes:**
   - **`LOCAL_ONLY` Mode:** Complete offline capability. All data remains on the device (embedded H2 file DB or Room SQLite). Supabase and Render are never contacted, and reconnecting to the internet will **never** automatically upload or leak data.
   - **`CLOUD_SYNC` Mode:** Operations are committed locally and safely propagated to the central Supabase PostgreSQL cluster through the Spring Boot REST API.
2. **Movement-Based Inventory Integrity:**
   - Stock balances are calculated from an immutable ledger (`STOCK_IN`, `SALE`, `PURCHASE`, `ADJUSTMENT`, `RETURN`).
   - Additive multi-device reconciliation prevents destructive last-write-wins race conditions when multiple offline devices make sales simultaneously.
3. **Multi-Tenant Isolation:**
   - Every entity is strictly bounded by `organization_id`.
   - Double-shielded security: enforced at the application service layer and directly inside PostgreSQL via Supabase **Row Level Security (RLS)**.
4. **Zero-Secret Client Footprint:**
   - The `SUPABASE_SERVICE_ROLE_KEY` is maintained exclusively server-side in Render environment variables. It is never embedded in client binaries, web JavaScript, or the Android APK.

---

## 2. Technology Stack

- **Backend:** Java 21, Spring Boot 3.2.3, Spring Data JPA, Hibernate, Spring Security, Apache POI 5.2.5 (Excel), iText 8.0.2 (PDF), SpringDoc OpenAPI (Swagger 3.0).
- **Web Frontend:** Server-Side Rendered (SSR) Thymeleaf, HTML5, Vanilla CSS Design System, JavaScript micro-interactions.
- **Desktop (Windows):** Spring Boot portable executable launcher, custom minimal JRE 21 runtime, WiX Toolset 3.14 MSI installer.
- **Mobile (Android):** Kotlin 2.0.0, Jetpack Compose, Material3, Room Database 2.6.1, Retrofit 2.11.0, OkHttp 4.12.0, Android WorkManager 2.9.1.
- **Databases:**
  - *Local Offline:* Embedded H2 Database (v2.2.224, file-based `./data/invoicedb.mv.db`) & Room (Android).
  - *Cloud Central:* Supabase PostgreSQL 15 with composite performance indexes and Row Level Security.
- **Deployment & Cloud:** Docker multi-stage build, Render Web Services, GitHub Actions.

---

## 3. Repository Structure

```
Invoice-Billing-Application/
├── android/                         # Native Android Gradle Project
│   ├── app/                         # Jetpack Compose UI, Room DB, Retrofit
│   │   ├── src/main/java/...        # Kotlin source files
│   │   └── build.gradle.kts         # App dependencies (Room, KSP, Compose)
│   ├── build.gradle.kts             # Root Gradle build script
│   └── settings.gradle.kts          # Gradle plugin repositories
├── database/                        # Central PostgreSQL & Supabase DDL
│   ├── schema.sql                   # 23 core relational tables & constraints
│   ├── sync_schema.sql              # Sync queue & idempotency tables
│   ├── indexes.sql                  # Performance & tenant isolation indexes
│   ├── rls.sql                      # Row Level Security (RLS) policies
│   └── seed.sql                     # Seed data & sequence resets
├── docs/                            # Comprehensive Documentation
│   ├── ARCHITECTURE.md              # System design & component diagrams
│   ├── API.md                       # OpenAPI & REST API specifications
│   ├── SUPABASE_SETUP.md            # Exact execution order & verification queries
│   ├── RENDER_DEPLOYMENT.md         # Render Web Service deployment guide
│   ├── ANDROID_BUILD.md             # Android build, signing, and installation
│   ├── OFFLINE_MODE.md              # Local-first zero leakage guarantees
│   ├── SYNC.md                      # Synchronization engine & idempotency
│   └── FINAL_RELEASE_REPORT.md      # Verification matrix and release artifacts
├── src/                             # Spring Boot Source Code
│   ├── main/java/...                # Controllers, models, services, repositories
│   └── main/resources/              # Thymeleaf templates, static CSS/JS, configs
├── Dockerfile                       # Multi-stage Java 21 Alpine container
├── render.yaml                      # Render Blueprint specification
├── pom.xml                          # Maven build configuration
├── package.bat                      # Windows jpackage desktop bundler
├── run.bat                          # Local development launcher
├── clean.bat                        # Artifact cleanup script
├── .env.example                     # Environment configuration template
└── .gitignore                       # Production gitignore rules
```

---

## 4. Local Setup & Quick Start

### Prerequisites
- **Java Development Kit (JDK):** Version 21 (Eclipse Temurin or Microsoft OpenJDK).
- **Maven:** Version 3.9+ (or use the included `apache-maven-3.9.6/`).

### Option A: Running the Spring Boot Backend (Local Mode)
1. Clone the repository:
   ```bash
   git clone https://github.com/singhshubham4422/SwiftLab.git
   cd SwiftLab
   ```
2. Build and run tests:
   ```bash
   mvn clean test
   ```
3. Launch the application:
   ```bash
   mvn spring-boot:run
   ```
4. Access the application in your browser:
   - **Web UI:** `http://localhost:8080`
   - **Swagger OpenAPI:** `http://localhost:8080/swagger-ui.html`
   - **API Docs (JSON):** `http://localhost:8080/v3/api-docs`

---

## 5. Database Setup (Supabase)

To provision your central cloud database on [Supabase](https://supabase.com):

1. Create a new Supabase project.
2. Open the **SQL Editor** in your Supabase dashboard.
3. Execute the SQL scripts in this **exact order**:
   1. [`database/schema.sql`](database/schema.sql)
   2. [`database/sync_schema.sql`](database/sync_schema.sql)
   3. [`database/indexes.sql`](database/indexes.sql)
   4. [`database/rls.sql`](database/rls.sql)
   5. [`database/seed.sql`](database/seed.sql) *(Optional: demo organization & initial admin user)*

Refer to [`docs/SUPABASE_SETUP.md`](docs/SUPABASE_SETUP.md) for full instructions, verification queries, and isolation tests.

---

## 6. Android Mobile Client Setup & Build

The native Android client is located in [`/android/`](android/).

### Prerequisites
- Android SDK (API Level 36 or 35).
- Build Tools 36.0.0.
- JDK 17 or JDK 21.

### Build Commands:
```powershell
cd android

# Build Debug APK
.\gradlew.bat assembleDebug
# Generated at: android/app/build/outputs/apk/debug/app-debug.apk

# Build Release APK
.\gradlew.bat assembleRelease
# Generated at: android/app/build/outputs/apk/release/app-release.apk
```

Refer to [`docs/ANDROID_BUILD.md`](docs/ANDROID_BUILD.md) for keystore signing instructions and installation options.

---

## 7. Windows Desktop Packaging

To create a standalone, native Windows distribution:
```cmd
package.bat
```
This produces:
- **Portable Folder:** `dist/InvoiceApp/InvoiceApp.exe` (with embedded minimal JRE runtime).
- **WiX Installer:** `dist/InvoiceApp-Installer.exe` (single-file setup package).

---

## 8. Render Production Deployment

1. Push your repository to GitHub:
   ```bash
   git push origin main
   ```
2. In the [Render Dashboard](https://dashboard.render.com), create a new **Web Service** and connect your GitHub repository.
3. Select **Docker** environment.
4. Set the following required environment variables:
   - `SPRING_PROFILES_ACTIVE`: `cloud`
   - `DATABASE_URL`: `jdbc:postgresql://db.<PROJECT_REF>.supabase.co:5432/postgres?sslmode=require`
   - `DATABASE_USERNAME`: `postgres`
   - `DATABASE_PASSWORD`: `<YOUR_SUPABASE_PASSWORD>`
   - `SUPABASE_URL`: `https://<PROJECT_REF>.supabase.co`
   - `SUPABASE_ANON_KEY`: `<YOUR_ANON_KEY>`
   - `SUPABASE_SERVICE_ROLE_KEY`: `<YOUR_SERVICE_ROLE_KEY>`
   - `APP_JWT_SECRET`: `<SECURE_32_CHAR_STRING>`
5. Click **Create Web Service**.

Refer to [`docs/RENDER_DEPLOYMENT.md`](docs/RENDER_DEPLOYMENT.md) for health checks and verification steps.

---

## 9. Environment Variables Reference

See [`.env.example`](.env.example) for a complete template:

| Variable Name | Required For | Description |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Cloud / Render | Set to `local` (H2) or `cloud` (Supabase). |
| `DATABASE_URL` | Cloud / Render | JDBC connection string to Supabase PostgreSQL. |
| `DATABASE_USERNAME` | Cloud / Render | Database user (`postgres`). |
| `DATABASE_PASSWORD` | Cloud / Render | Database password. |
| `SUPABASE_URL` | Cloud / Render | Supabase project REST API endpoint. |
| `SUPABASE_ANON_KEY` | Cloud / Render | Supabase public anonymous client key. |
| `SUPABASE_SERVICE_ROLE_KEY` | Cloud / Render | Supabase administrative key (server-side ONLY). |
| `APP_JWT_SECRET` | All Profiles | 32+ character key for signing API tokens. |
| `APP_CLOUD_API_URL` | Desktop / Mobile | Remote backend URL when syncing from clients. |
| `PORT` | Render | Server HTTP port (dynamically set by Render). |

---

## 10. Automated Test Suite

Run the full integration test suite:
```bash
mvn clean test
```
The test suite validates 10 key operational areas:
1. `AuthAndSecurityTest`: BCrypt hashing, legacy SHA-256 transparent upgrade, API token lifecycle.
2. `OrganizationIsolationTest`: Multi-tenant isolation across products, customers, suppliers, and invoices.
3. `ProductCatalogTest`: SKU uniqueness, catalog CRUD, soft-delete filtering.
4. `InventoryEngineTest`: Movement ledger (+100 -5 +20 +2 -1 = 116 units), negative stock guard, concurrency.
5. `PurchaseWorkflowTest`: Purchase order creation, goods receipt, automated inventory replenishment.
6. `SaleWorkflowTest`: POS checkout, stock depletion, auto-invoicing, payments.
7. `InvoiceAndPaymentTest`: Tax/discount calculations, partial payments, status transitions, PDF export.
8. `SyncAndLocalModeTest`: `LOCAL_ONLY` zero-leakage guarantee, mode switch confirmation, `AppliedSyncKey` idempotency.

---

## 11. Known Limitations & Manual Actions

1. **Android Play Store Distribution:** The release APK is generated with default staging signing configs. Production publishing requires generating a private keystore via `keytool` and signing with `apksigner` as detailed in [`docs/ANDROID_BUILD.md`](docs/ANDROID_BUILD.md).
2. **Initial Cloud Database Execution:** The SQL scripts in `/database/` must be manually run in the Supabase SQL Editor once before deploying to Render.
3. **Render GitHub Authorization:** Render requires manual connection to your GitHub account to enable automatic CI/CD web service deploys.
