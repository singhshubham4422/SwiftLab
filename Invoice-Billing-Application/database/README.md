# SwiftLab Database Architecture & Setup

## Overview

SwiftLab Inventory & Billing Platform uses a dual-database design:
1. **Local Storage (Windows/Desktop/Offline):** Embedded H2 file database (`./data/invoicedb`). High performance, completely zero-network offline capability, persistent across reboots.
2. **Cloud Storage (Supabase PostgreSQL):** Central source of truth for multi-device sync, tenant isolation, and web/API access.

---

## File Directory

- `schema.sql`: Full DDL defining all 25 relational tables (organizations, users, catalog, movement-based inventory, sales, purchases, invoices, payments, sync queue, audit logs).
- `indexes.sql`: Performance indexes on foreign keys, tenant lookups, date ranges, and status fields.
- `seed.sql`: Initial seed data including demo organization, hashed owner credentials (`admin` / `admin123`), warehouse, units, categories, and partners.
- `sync_schema.sql`: Dedicated synchronization schema including queue and idempotency keys (`applied_sync_keys`).
- `rls.sql`: Row Level Security policies enforcing strict tenant isolation by `organization_id`.

---

## Supabase PostgreSQL Setup Instructions

1. **Create Supabase Project:** Log into [supabase.com](https://supabase.com) and create a new project.
2. **Open SQL Editor:** Navigate to the SQL Editor in your Supabase dashboard.
3. **Execute SQL in Order:**
   - Run `schema.sql` to generate all tables and constraints.
   - Run `indexes.sql` to install performance and foreign key indexes.
   - Run `sync_schema.sql` to ensure idempotency tables are prepared.
   - Run `rls.sql` to activate Row Level Security and organization isolation.
   - (Optional) Run `seed.sql` to populate sample data.
4. **Configure Environment Variables:**
   Add your Supabase PostgreSQL credentials to `.env` or application runtime variables:
   ```properties
   SPRING_PROFILES_ACTIVE=cloud
   SPRING_DATASOURCE_URL=jdbc:postgresql://db.<supabase-ref>.supabase.co:5432/postgres
   SPRING_DATASOURCE_USERNAME=postgres
   SPRING_DATASOURCE_PASSWORD=your_secure_password
   ```

---

## Local H2 Database (Default)

When running locally without cloud variables or with `spring.profiles.active=local`, the application automatically uses the embedded file database located at `./data/invoicedb.mv.db`.
All tables are automatically managed via Hibernate `ddl-auto: update`.
