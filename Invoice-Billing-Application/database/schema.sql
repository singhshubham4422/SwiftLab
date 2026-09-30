-- ==============================================================================
-- SwiftLab Inventory & Billing Platform - Cloud Database Schema (Supabase / PostgreSQL)
-- Multi-Tenant, Movement-Based Inventory, Dual Data Mode (Local / Cloud)
-- ==============================================================================

-- 1. Organizations
CREATE TABLE IF NOT EXISTS organizations (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    currency VARCHAR(10) DEFAULT 'USD',
    country VARCHAR(100),
    tax_id VARCHAR(50),
    plan VARCHAR(50) DEFAULT 'STANDARD',
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 2. Users
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT REFERENCES organizations(id) ON DELETE CASCADE,
    username VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(150),
    email VARCHAR(150),
    organization_name VARCHAR(255),
    phone VARCHAR(50),
    role VARCHAR(50) NOT NULL DEFAULT 'STAFF',
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 3. Company Settings
CREATE TABLE IF NOT EXISTS company_settings (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT REFERENCES organizations(id) ON DELETE CASCADE,
    user_id BIGINT,
    company_name VARCHAR(255) NOT NULL,
    tagline VARCHAR(255),
    address TEXT,
    phone VARCHAR(50),
    email VARCHAR(150),
    website VARCHAR(150),
    tax_id VARCHAR(50),
    invoice_prefix VARCHAR(20) DEFAULT 'INV-',
    default_tax_rate NUMERIC(10,2) DEFAULT 0.00,
    currency_symbol VARCHAR(10) DEFAULT '$',
    invoice_footer_notes TEXT,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 4. Application Runtime State
CREATE TABLE IF NOT EXISTS app_runtime (
    id BIGSERIAL PRIMARY KEY,
    device_id VARCHAR(100) NOT NULL,
    platform VARCHAR(50) DEFAULT 'WINDOWS',
    current_mode VARCHAR(50) DEFAULT 'LOCAL_ONLY',
    organization_id BIGINT REFERENCES organizations(id) ON DELETE SET NULL,
    active_user_id BIGINT,
    cloud_sync_enabled BOOLEAN DEFAULT FALSE,
    last_successful_sync_at TIMESTAMPTZ,
    pending_sync_count INT DEFAULT 0,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 5. Devices
CREATE TABLE IF NOT EXISTS devices (
    id BIGSERIAL PRIMARY KEY,
    device_id VARCHAR(100) UNIQUE NOT NULL,
    organization_id BIGINT REFERENCES organizations(id) ON DELETE CASCADE,
    name VARCHAR(150),
    platform VARCHAR(50) DEFAULT 'WINDOWS',
    last_seen_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 6. API Tokens
CREATE TABLE IF NOT EXISTS api_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(255) UNIQUE NOT NULL,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    organization_id BIGINT REFERENCES organizations(id) ON DELETE CASCADE,
    device_id VARCHAR(100),
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked BOOLEAN DEFAULT FALSE
);

-- 7. Categories
CREATE TABLE IF NOT EXISTS categories (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT REFERENCES organizations(id) ON DELETE CASCADE,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_cat_org_name UNIQUE (organization_id, name)
);

-- 8. Brands
CREATE TABLE IF NOT EXISTS brands (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT REFERENCES organizations(id) ON DELETE CASCADE,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_brand_org_name UNIQUE (organization_id, name)
);

-- 9. Units
CREATE TABLE IF NOT EXISTS units (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT REFERENCES organizations(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    symbol VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_unit_org_name UNIQUE (organization_id, name)
);

-- 10. Products
CREATE TABLE IF NOT EXISTS products (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT REFERENCES organizations(id) ON DELETE CASCADE,
    sku VARCHAR(100) NOT NULL,
    barcode VARCHAR(100),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category_id BIGINT REFERENCES categories(id) ON DELETE SET NULL,
    brand_id BIGINT REFERENCES brands(id) ON DELETE SET NULL,
    unit_id BIGINT REFERENCES units(id) ON DELETE SET NULL,
    cost_price NUMERIC(15,2) DEFAULT 0.00,
    selling_price NUMERIC(15,2) DEFAULT 0.00,
    alert_quantity NUMERIC(15,2) DEFAULT 5.00,
    active BOOLEAN DEFAULT TRUE,
    deleted BOOLEAN DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_product_org_sku UNIQUE (organization_id, sku)
);

-- 11. Warehouses
CREATE TABLE IF NOT EXISTS warehouses (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT REFERENCES organizations(id) ON DELETE CASCADE,
    name VARCHAR(150) NOT NULL,
    code VARCHAR(50) NOT NULL,
    address TEXT,
    is_default BOOLEAN DEFAULT FALSE,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_warehouse_org_code UNIQUE (organization_id, code)
);

-- 12. Stocks
CREATE TABLE IF NOT EXISTS stocks (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT REFERENCES organizations(id) ON DELETE CASCADE,
    warehouse_id BIGINT REFERENCES warehouses(id) ON DELETE CASCADE,
    product_id BIGINT REFERENCES products(id) ON DELETE CASCADE,
    quantity NUMERIC(15,2) DEFAULT 0.00,
    reserved_quantity NUMERIC(15,2) DEFAULT 0.00,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_stock_org_wh_prod UNIQUE (organization_id, warehouse_id, product_id)
);

-- 13. Stock Movements (Ledger)
CREATE TABLE IF NOT EXISTS stock_movements (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT REFERENCES organizations(id) ON DELETE CASCADE,
    warehouse_id BIGINT REFERENCES warehouses(id) ON DELETE CASCADE,
    product_id BIGINT REFERENCES products(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,
    quantity NUMERIC(15,2) NOT NULL,
    balance_after NUMERIC(15,2) NOT NULL,
    reference_type VARCHAR(50),
    reference_id BIGINT,
    notes TEXT,
    device_id VARCHAR(100),
    created_by VARCHAR(100),
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 14. Customers
CREATE TABLE IF NOT EXISTS customers (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT REFERENCES organizations(id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    email VARCHAR(150),
    phone VARCHAR(50),
    address TEXT,
    tax_number VARCHAR(50),
    active BOOLEAN DEFAULT TRUE,
    deleted BOOLEAN DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 15. Suppliers
CREATE TABLE IF NOT EXISTS suppliers (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT REFERENCES organizations(id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    email VARCHAR(150),
    phone VARCHAR(50),
    address TEXT,
    tax_number VARCHAR(50),
    active BOOLEAN DEFAULT TRUE,
    deleted BOOLEAN DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 16. Purchases
CREATE TABLE IF NOT EXISTS purchases (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT REFERENCES organizations(id) ON DELETE CASCADE,
    supplier_id BIGINT REFERENCES suppliers(id) ON DELETE RESTRICT,
    warehouse_id BIGINT REFERENCES warehouses(id) ON DELETE RESTRICT,
    purchase_number VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ORDERED',
    total_amount NUMERIC(15,2) DEFAULT 0.00,
    paid_amount NUMERIC(15,2) DEFAULT 0.00,
    notes TEXT,
    created_by VARCHAR(100),
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_purchase_org_num UNIQUE (organization_id, purchase_number)
);

-- 17. Purchase Items
CREATE TABLE IF NOT EXISTS purchase_items (
    id BIGSERIAL PRIMARY KEY,
    purchase_id BIGINT REFERENCES purchases(id) ON DELETE CASCADE,
    product_id BIGINT REFERENCES products(id) ON DELETE RESTRICT,
    quantity NUMERIC(15,2) NOT NULL,
    unit_cost NUMERIC(15,2) NOT NULL,
    subtotal NUMERIC(15,2) NOT NULL
);

-- 18. Sales
CREATE TABLE IF NOT EXISTS sales (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT REFERENCES organizations(id) ON DELETE CASCADE,
    customer_id BIGINT REFERENCES customers(id) ON DELETE SET NULL,
    warehouse_id BIGINT REFERENCES warehouses(id) ON DELETE RESTRICT,
    sale_number VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'COMPLETED',
    total_amount NUMERIC(15,2) DEFAULT 0.00,
    paid_amount NUMERIC(15,2) DEFAULT 0.00,
    invoice_id BIGINT,
    notes TEXT,
    device_id VARCHAR(100),
    created_by VARCHAR(100),
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_sale_org_num UNIQUE (organization_id, sale_number)
);

-- 19. Sale Items
CREATE TABLE IF NOT EXISTS sale_items (
    id BIGSERIAL PRIMARY KEY,
    sale_id BIGINT REFERENCES sales(id) ON DELETE CASCADE,
    product_id BIGINT REFERENCES products(id) ON DELETE RESTRICT,
    quantity NUMERIC(15,2) NOT NULL,
    unit_price NUMERIC(15,2) NOT NULL,
    subtotal NUMERIC(15,2) NOT NULL
);

-- 20. Invoices
CREATE TABLE IF NOT EXISTS invoices (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT REFERENCES organizations(id) ON DELETE CASCADE,
    user_id BIGINT,
    sale_id BIGINT,
    invoice_number VARCHAR(100) NOT NULL,
    customer_name VARCHAR(200),
    customer_email VARCHAR(150),
    customer_address TEXT,
    customer_phone VARCHAR(50),
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    issue_date DATE,
    due_date DATE,
    payment_terms VARCHAR(100),
    subtotal DOUBLE PRECISION DEFAULT 0.0,
    tax_rate DOUBLE PRECISION DEFAULT 0.0,
    tax_amount DOUBLE PRECISION DEFAULT 0.0,
    discount DOUBLE PRECISION DEFAULT 0.0,
    total DOUBLE PRECISION DEFAULT 0.0,
    notes TEXT,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_invoice_org_num UNIQUE (organization_id, invoice_number)
);

-- 21. Invoice Items
CREATE TABLE IF NOT EXISTS invoice_items (
    id BIGSERIAL PRIMARY KEY,
    invoice_id BIGINT REFERENCES invoices(id) ON DELETE CASCADE,
    product_id BIGINT,
    description VARCHAR(255),
    quantity INT NOT NULL,
    unit_price DOUBLE PRECISION NOT NULL,
    total DOUBLE PRECISION NOT NULL
);

-- 22. Payments
CREATE TABLE IF NOT EXISTS payments (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT REFERENCES organizations(id) ON DELETE CASCADE,
    invoice_id BIGINT REFERENCES invoices(id) ON DELETE SET NULL,
    sale_id BIGINT REFERENCES sales(id) ON DELETE SET NULL,
    payment_number VARCHAR(100) NOT NULL,
    amount NUMERIC(15,2) NOT NULL,
    method VARCHAR(50) NOT NULL DEFAULT 'CASH',
    payment_date TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    reference_number VARCHAR(100),
    notes TEXT,
    created_by VARCHAR(100),
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 23. Sync Queue
CREATE TABLE IF NOT EXISTS sync_queue (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT REFERENCES organizations(id) ON DELETE CASCADE,
    device_id VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id BIGINT NOT NULL,
    operation VARCHAR(50) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    retry_count INT DEFAULT 0,
    last_attempt_at TIMESTAMPTZ,
    error_message TEXT,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 24. Applied Sync Keys (Idempotency)
CREATE TABLE IF NOT EXISTS applied_sync_keys (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT REFERENCES organizations(id) ON DELETE CASCADE,
    sync_key VARCHAR(255) UNIQUE NOT NULL,
    entity_type VARCHAR(100),
    entity_id BIGINT,
    operation VARCHAR(50),
    applied_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 25. Audit Logs
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT,
    user_id BIGINT,
    device_id VARCHAR(100),
    username VARCHAR(100),
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100),
    entity_id BIGINT,
    metadata TEXT,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);
