-- ==============================================================================
-- SwiftLab Inventory & Billing Platform - Performance & Isolation Indexes
-- ==============================================================================

-- Tenancy & User Indexes
CREATE INDEX IF NOT EXISTS idx_users_org_id ON users(organization_id);
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_api_tokens_lookup ON api_tokens(token, revoked, expires_at);
CREATE INDEX IF NOT EXISTS idx_devices_org_dev ON devices(organization_id, device_id);

-- Product & Catalog Indexes
CREATE INDEX IF NOT EXISTS idx_products_org_id ON products(organization_id);
CREATE INDEX IF NOT EXISTS idx_products_org_sku ON products(organization_id, sku);
CREATE INDEX IF NOT EXISTS idx_products_category ON products(organization_id, category_id);
CREATE INDEX IF NOT EXISTS idx_products_brand ON products(organization_id, brand_id);
CREATE INDEX IF NOT EXISTS idx_products_deleted ON products(organization_id, deleted);

-- Warehouse & Inventory Indexes
CREATE INDEX IF NOT EXISTS idx_warehouses_org ON warehouses(organization_id);
CREATE INDEX IF NOT EXISTS idx_stocks_lookup ON stocks(organization_id, warehouse_id, product_id);
CREATE INDEX IF NOT EXISTS idx_movements_lookup ON stock_movements(organization_id, product_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_movements_ref ON stock_movements(reference_type, reference_id);

-- CRM Indexes
CREATE INDEX IF NOT EXISTS idx_customers_org ON customers(organization_id, deleted);
CREATE INDEX IF NOT EXISTS idx_suppliers_org ON suppliers(organization_id, deleted);

-- Purchase & Sale Indexes
CREATE INDEX IF NOT EXISTS idx_purchases_org ON purchases(organization_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_purchase_items_pid ON purchase_items(purchase_id);
CREATE INDEX IF NOT EXISTS idx_sales_org ON sales(organization_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_sale_items_sid ON sale_items(sale_id);

-- Invoices & Payments Indexes
CREATE INDEX IF NOT EXISTS idx_invoices_org_status ON invoices(organization_id, status);
CREATE INDEX IF NOT EXISTS idx_invoices_org_num ON invoices(organization_id, invoice_number);
CREATE INDEX IF NOT EXISTS idx_invoices_dates ON invoices(organization_id, issue_date DESC);
CREATE INDEX IF NOT EXISTS idx_invoice_items_iid ON invoice_items(invoice_id);
CREATE INDEX IF NOT EXISTS idx_payments_org_inv ON payments(organization_id, invoice_id);

-- Synchronization & Audit Indexes
CREATE INDEX IF NOT EXISTS idx_sync_queue_lookup ON sync_queue(organization_id, status, retry_count);
CREATE INDEX IF NOT EXISTS idx_applied_sync_keys_key ON applied_sync_keys(organization_id, sync_key);
CREATE INDEX IF NOT EXISTS idx_audit_logs_lookup ON audit_logs(organization_id, created_at DESC);
