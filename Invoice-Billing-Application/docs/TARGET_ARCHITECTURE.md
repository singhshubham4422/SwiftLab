# Target Architecture — SwiftLab Inventory & Billing Platform

## Principle

**Local-first, optional cloud sync.** Business rules live only in Spring Boot services. Web, Windows (embedded Spring Boot), and a future Android client consume those rules. Local-only data is never uploaded because the network is up; the operator must opt into **Cloud Sync**.

## Deployment modes

### Web (hosted)

```
Browser → Spring Boot (Thymeleaf + REST) → PostgreSQL (Supabase)
```

Profile: `cloud`. No H2 requirement.

### Windows (.exe)

```
InvoiceApp.exe
  → embedded Spring Boot
  → H2 file database (not in-memory)
  → if Cloud Sync: SyncQueue → remote Spring Boot API → Supabase PostgreSQL
```

Works offline. Profile: `local`.

### Android (future APK — not built in this repo)

```
Kotlin app → local Room/SQLite → Sync Manager → Spring Boot REST → PostgreSQL
```

See `docs/ANDROID_CLIENT_PLAN.md`. No Flutter.

## Two explicit data modes

| Mode | Behavior |
| --- | --- |
| **LOCAL_ONLY** | No cloud HTTP, no Supabase, no SyncQueue processing. Banner: LOCAL ONLY. |
| **CLOUD_SYNC** | Local writes + SyncQueue. When online, push/pull via Spring API. Banner: ONLINE/OFFLINE + queue depth. |

Switching to Cloud Sync requires confirmation. Records created while Local Only are **not** queued unless the operator later opts in.

## Organization isolation

Every business row carries `organization_id`. APIs and services load the current user’s organization and reject cross-org access. PostgreSQL RLS in `database/rls.sql` is a second line of defense for direct database clients.

## Identity and security

- BCrypt (Spring Security `PasswordEncoder`). Legacy SHA-256 hashes are re-hashed on successful login.
- Web: form login + session + CSRF.
- API: opaque bearer tokens (`/api/auth/login`) for Android/future clients.
- Roles: OWNER, ADMIN, MANAGER, STAFF enforced in services (`AccessPolicy`), not only in the UI.

## Inventory

Stock quantity is derived/maintained from **StockMovement** (`STOCK_IN`, `STOCK_OUT`, `SALE`, `PURCHASE`, `RETURN`, `ADJUSTMENT`, `TRANSFER`). Sales cannot oversell unless the organization allows negative inventory.

## Sync and conflicts

- Device ID: random UUID persisted on first run (not hardware serial).
- SyncQueue is idempotent on `(organization_id, public_id, operation)`.
- **Stock:** append all valid movements; never last-timestamp-wins on quantity.
- **Master data:** last `updated_at` + `version` with documented tie-break (`docs/SYNC_CONFLICTS.md`).
- Soft delete (`deleted`, `deleted_at`) for synchronized entities.

## Unchanged capabilities (refactored, not dropped)

Invoice PDF, Excel import/export, invoice numbering, business settings, Windows launcher/packaging, login/signup UX.
