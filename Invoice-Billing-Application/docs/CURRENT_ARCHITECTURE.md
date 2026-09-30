# Current Architecture (as inspected)

This document describes the **existing** SwiftLab Invoice Billing Application before the inventory-and-billing platform migration.

## Stack

| Layer | Technology |
| --- | --- |
| Language | Java 21 |
| Framework | Spring Boot 3.2.3 |
| Web | Spring MVC + Thymeleaf |
| Persistence | Spring Data JPA / Hibernate, H2 **file** DB (`./data/invoice-db`) |
| Excel | Apache POI 5.2.5 |
| PDF | iText 7.2.5 |
| Build | Maven (`pom.xml` + bundled `apache-maven-3.9.6`) |
| Desktop | `run.bat`, `package.bat` (jpackage + WiX), `clean.bat` |

There is **no** Android project, **no** Spring Security, **no** REST API layer, **no** automated tests under `src/test`.

## Runtime shape

```
Browser (localhost:8080)
    → Spring MVC controllers
    → Services
    → H2 file database
Optional: async HTTP POST to Supabase REST (`/rest/v1/invoices`)
```

On `ApplicationReadyEvent`, a non-headless JVM opens `http://localhost:8080/invoices`.

## Packages and responsibilities

- `InvoiceApplication` — boot, `@EnableAsync`, `@EnableScheduling`, desktop browser launch.
- `controller.AuthController` — session login/signup/logout.
- `controller.AuthInterceptor` + `WebMvcConfig` — session gate (not Spring Security).
- `controller.InvoiceController` — dashboard, CRUD, PDF/Excel, import, settings, manual Supabase sync.
- `model` — `User`, `Invoice`, `InvoiceItem`, `CompanySettings`.
- `repository` — JPA repositories.
- `service` — invoice numbering, PDF/Excel, SHA-256 passwords, Supabase HTTP client.

## What works and should be reused

- Thymeleaf UI, theme CSS/JS, login/signup/settings/invoice form/detail/import.
- Invoice line items, tax/discount totals, statuses PENDING/PAID/OVERDUE.
- PDF and Excel export, Excel template + import.
- Company branding (name, currency, tax, footer).
- H2 **file** persistence (survives restart).
- jpackage Windows packaging (`package.bat`, WiX on PATH).
- Best-effort offline invoice save if Supabase is down.

## What must be modified

- `User` stores `organizationName` as a string, not an Organization entity.
- Invoices scoped by `userId` only — not organization isolation.
- Passwords hashed with **SHA-256** (not suitable for production).
- Invoice list/edit/delete do not verify the invoice belongs to the session user (IDOR risk).
- Supabase keys stored on `CompanySettings`; auto-sync can upload whenever URL+key exist.
- Invoice-only domain (no products, stock movements, customers as entities, suppliers, purchases, sales, payments).
- Sync is last-write merge on `invoice_number`, not movement-based or queued.
- No roles, devices, audit log, OpenAPI, or dual data modes.

## What must be replaced

- SHA-256 as the production password scheme → BCrypt via Spring Security.
- Interceptor-only security as the sole authorization mechanism → Spring Security + service-level org/role checks.
- Direct “always auto-sync if configured” behavior → explicit **Local Only** vs **Cloud Sync**.
- Manual `product.quantity`-style inventory (not present yet) must never be the source of truth; movements will be.

## What is missing

Organization, roles (OWNER/ADMIN/MANAGER/STAFF), Device, Product/Category/Brand/Unit, Warehouse/Stock/StockMovement, Customer, Supplier, Purchase/Sale, Payment, SyncQueue, AuditLog, REST API, Swagger, PostgreSQL/cloud profile, RLS SQL pack, tests, mode banner, Android client contract.

## Packaging notes

- `run.bat` launches `target\invoice-app-1.0.0.jar` with `javaw` and opens the browser.
- `package.bat` uses jpackage `app-image` and `exe` with `JarLauncher`.
- Local DB path is project-relative `./data/` — acceptable for portable app if working directory is the install dir; must not move to temp.
