# SwiftLab Central Supabase Database Setup & Verification Guide

This document outlines the **exact, safe execution order**, prerequisites, verification queries, and multi-tenant isolation validation steps for initializing the central PostgreSQL database on Supabase.

---

## 1. Prerequisites

1. An active account at [https://supabase.com](https://supabase.com).
2. A new or existing Supabase project (e.g. `swiftlab-production`).
3. Note your project credentials:
   - **Host / JDBC URL:** `jdbc:postgresql://db.<PROJECT_REF>.supabase.co:5432/postgres?sslmode=require`
   - **Database Password:** Set during project creation.
   - **Project URL:** `https://<PROJECT_REF>.supabase.co`
   - **Anon Public Key:** `eyJhbGciOi...`
   - **Service Role Key:** (Keep strictly server-side in Render environment variables; NEVER expose to client applications).

---

## 2. Safe SQL Execution Order

Open the **SQL Editor** in your Supabase dashboard (`https://supabase.com/dashboard/project/<PROJECT_REF>/sql/new`).
Run the SQL scripts strictly in this sequential order:

```
Step 1: database/schema.sql       -> Core relational tables & constraints (23 tables)
Step 2: database/sync_schema.sql  -> Sync queue & idempotency tables (2 tables)
Step 3: database/indexes.sql      -> Composite performance & isolation indexes
Step 4: database/rls.sql          -> Enable Row Level Security & multi-tenant policies
Step 5: database/seed.sql         -> (Optional) Bootstrap demo organization & admin user
```

### Detailed Execution Steps:

### Step 1: Run `database/schema.sql`
- **Location:** [`database/schema.sql`](file:///c:/Users/sshub/Music/SwiftLab/Invoice-Billing-Application/database/schema.sql)
- **Purpose:** Creates core relational tables: `organizations`, `users`, `company_settings`, `app_runtime`, `devices`, `api_tokens`, `units`, `categories`, `brands`, `products`, `warehouses`, `stocks`, `stock_movements`, `customers`, `suppliers`, `purchases`, `purchase_items`, `sales`, `sale_items`, `invoices`, `invoice_items`, `payments`, `audit_logs`.
- **Expected Result:** `Success. No rows returned.`

### Step 2: Run `database/sync_schema.sql`
- **Location:** [`database/sync_schema.sql`](file:///c:/Users/sshub/Music/SwiftLab/Invoice-Billing-Application/database/sync_schema.sql)
- **Purpose:** Creates `sync_queue` and `applied_sync_keys` tables required for multi-device offline sync and deduplication.
- **Why here:** `indexes.sql` and `rls.sql` depend on these two tables existing.
- **Expected Result:** `Success. No rows returned.`

### Step 3: Run `database/indexes.sql`
- **Location:** [`database/indexes.sql`](file:///c:/Users/sshub/Music/SwiftLab/Invoice-Billing-Application/database/indexes.sql)
- **Purpose:** Creates foreign key lookups and composite tenant-isolation indexes (`organization_id, sku`, `organization_id, warehouse_id, product_id`, etc.) across all 25 tables.
- **Expected Result:** `Success. No rows returned.`

### Step 4: Run `database/rls.sql`
- **Location:** [`database/rls.sql`](file:///c:/Users/sshub/Music/SwiftLab/Invoice-Billing-Application/database/rls.sql)
- **Purpose:** Enables Row Level Security on all tenant tables and defines `current_org_id()` filter policies. Safe to re-run (idempotent `DROP POLICY IF EXISTS`).
- **Expected Result:** `Success. No rows returned.`

### Step 5: (Optional) Run `database/seed.sql`
- **Location:** [`database/seed.sql`](file:///c:/Users/sshub/Music/SwiftLab/Invoice-Billing-Application/database/seed.sql)
- **Purpose:** Bootstraps default organization #1 (`SwiftLab Demo Enterprise`), initial admin user (`admin` / `admin123` hashed via BCrypt), standard units, categories, brands, and resets PostgreSQL sequences.
- **Expected Result:** `Success. 9 rows affected.`

---

## 3. Post-Setup Verification Queries

Run the following queries in the Supabase SQL Editor to verify the installation:

### Verification Query 1: Table Count & Schema Check
```sql
SELECT count(*) AS total_tables 
FROM information_schema.tables 
WHERE table_schema = 'public' 
  AND table_type = 'BASE TABLE';
```
- **Expected Result:** `25` (23 core tables + `sync_queue` + `applied_sync_keys`).

### Verification Query 2: Row Level Security Status Check
```sql
SELECT tablename, rowsecurity 
FROM pg_tables 
WHERE schemaname = 'public'
ORDER BY tablename;
```
- **Expected Result:** All application tables report `rowsecurity = true`.

### Verification Query 3: Seed Data Validation
```sql
SELECT id, name, currency, plan, active FROM organizations;
SELECT id, organization_id, username, role, active FROM users;
```
- **Expected Result:** Organization 1 (`SwiftLab Demo Enterprise`) and User 1 (`admin`, role `OWNER`) are returned.

---

## 4. Multi-Tenant Isolation Testing Query

Execute this query in the SQL Editor to test tenant isolation under RLS:

```sql
-- Simulate session context for Organization 1
SET LOCAL "app.current_organization_id" = '1';
SELECT count(*) AS org1_products FROM products;

-- Simulate session context for non-existent Organization 999
SET LOCAL "app.current_organization_id" = '999';
SELECT count(*) AS org999_products FROM products;
```
- **Expected Result:** `org999_products` returns `0`, confirming that queries bounded to Organization 999 cannot access Organization 1's catalog or inventory.

---

## 5. Connecting Spring Boot Backend on Render

Once the SQL scripts have been executed, provide the database connection string to your Render Web Service environment:

```env
SPRING_PROFILES_ACTIVE=cloud
DATABASE_URL=jdbc:postgresql://db.<PROJECT_REF>.supabase.co:5432/postgres?sslmode=require
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=<YOUR_SUPABASE_DB_PASSWORD>
SUPABASE_URL=https://<PROJECT_REF>.supabase.co
SUPABASE_ANON_KEY=<YOUR_ANON_PUBLIC_KEY>
SUPABASE_SERVICE_ROLE_KEY=<YOUR_SERVICE_ROLE_KEY>
```
*(Never put the `SUPABASE_SERVICE_ROLE_KEY` in frontend code, Android APK, or Windows desktop build).*
