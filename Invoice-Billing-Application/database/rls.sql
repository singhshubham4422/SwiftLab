-- ==============================================================================
-- SwiftLab Inventory & Billing Platform - Supabase Row Level Security (RLS)
-- Enforces Strict Multi-Tenant Isolation by organization_id
-- Fully Idempotent (safe to run multiple times)
-- ==============================================================================

-- Enable RLS on all tenant-specific tables
ALTER TABLE organizations ENABLE ROW LEVEL SECURITY;
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE company_settings ENABLE ROW LEVEL SECURITY;
ALTER TABLE devices ENABLE ROW LEVEL SECURITY;
ALTER TABLE api_tokens ENABLE ROW LEVEL SECURITY;
ALTER TABLE categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE brands ENABLE ROW LEVEL SECURITY;
ALTER TABLE units ENABLE ROW LEVEL SECURITY;
ALTER TABLE products ENABLE ROW LEVEL SECURITY;
ALTER TABLE warehouses ENABLE ROW LEVEL SECURITY;
ALTER TABLE stocks ENABLE ROW LEVEL SECURITY;
ALTER TABLE stock_movements ENABLE ROW LEVEL SECURITY;
ALTER TABLE customers ENABLE ROW LEVEL SECURITY;
ALTER TABLE suppliers ENABLE ROW LEVEL SECURITY;
ALTER TABLE purchases ENABLE ROW LEVEL SECURITY;
ALTER TABLE purchase_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE sales ENABLE ROW LEVEL SECURITY;
ALTER TABLE sale_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE invoices ENABLE ROW LEVEL SECURITY;
ALTER TABLE invoice_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE payments ENABLE ROW LEVEL SECURITY;
ALTER TABLE sync_queue ENABLE ROW LEVEL SECURITY;
ALTER TABLE applied_sync_keys ENABLE ROW LEVEL SECURITY;
ALTER TABLE audit_logs ENABLE ROW LEVEL SECURITY;

-- Helper function to extract organization_id from current session or JWT claim
CREATE OR REPLACE FUNCTION current_org_id() RETURNS BIGINT AS $$
BEGIN
    RETURN COALESCE(
        NULLIF(current_setting('request.jwt.claims', true)::json->>'organization_id', '')::BIGINT,
        NULLIF(current_setting('app.current_organization_id', true), '')::BIGINT
    );
EXCEPTION WHEN OTHERS THEN
    RETURN NULL;
END;
$$ LANGUAGE plpgsql STABLE;

-- 1. Organizations Policy
DROP POLICY IF EXISTS org_isolation_organizations ON organizations;
CREATE POLICY org_isolation_organizations ON organizations
    FOR ALL
    USING (id = current_org_id() OR auth.role() = 'service_role');

-- 2. Users Policy
DROP POLICY IF EXISTS org_isolation_users ON users;
CREATE POLICY org_isolation_users ON users
    FOR ALL
    USING (organization_id = current_org_id() OR auth.role() = 'service_role');

-- 3. Company Settings Policy
DROP POLICY IF EXISTS org_isolation_company_settings ON company_settings;
CREATE POLICY org_isolation_company_settings ON company_settings
    FOR ALL
    USING (organization_id = current_org_id() OR auth.role() = 'service_role');

-- 4. Devices Policy
DROP POLICY IF EXISTS org_isolation_devices ON devices;
CREATE POLICY org_isolation_devices ON devices
    FOR ALL
    USING (organization_id = current_org_id() OR auth.role() = 'service_role');

-- 5. API Tokens Policy
DROP POLICY IF EXISTS org_isolation_api_tokens ON api_tokens;
CREATE POLICY org_isolation_api_tokens ON api_tokens
    FOR ALL
    USING (organization_id = current_org_id() OR auth.role() = 'service_role');

-- 6. Categories Policy
DROP POLICY IF EXISTS org_isolation_categories ON categories;
CREATE POLICY org_isolation_categories ON categories
    FOR ALL
    USING (organization_id = current_org_id() OR auth.role() = 'service_role');

-- 7. Brands Policy
DROP POLICY IF EXISTS org_isolation_brands ON brands;
CREATE POLICY org_isolation_brands ON brands
    FOR ALL
    USING (organization_id = current_org_id() OR auth.role() = 'service_role');

-- 8. Units Policy
DROP POLICY IF EXISTS org_isolation_units ON units;
CREATE POLICY org_isolation_units ON units
    FOR ALL
    USING (organization_id = current_org_id() OR auth.role() = 'service_role');

-- 9. Products Policy
DROP POLICY IF EXISTS org_isolation_products ON products;
CREATE POLICY org_isolation_products ON products
    FOR ALL
    USING (organization_id = current_org_id() OR auth.role() = 'service_role');

-- 10. Warehouses Policy
DROP POLICY IF EXISTS org_isolation_warehouses ON warehouses;
CREATE POLICY org_isolation_warehouses ON warehouses
    FOR ALL
    USING (organization_id = current_org_id() OR auth.role() = 'service_role');

-- 11. Stocks Policy
DROP POLICY IF EXISTS org_isolation_stocks ON stocks;
CREATE POLICY org_isolation_stocks ON stocks
    FOR ALL
    USING (organization_id = current_org_id() OR auth.role() = 'service_role');

-- 12. Stock Movements Policy
DROP POLICY IF EXISTS org_isolation_stock_movements ON stock_movements;
CREATE POLICY org_isolation_stock_movements ON stock_movements
    FOR ALL
    USING (organization_id = current_org_id() OR auth.role() = 'service_role');

-- 13. Customers Policy
DROP POLICY IF EXISTS org_isolation_customers ON customers;
CREATE POLICY org_isolation_customers ON customers
    FOR ALL
    USING (organization_id = current_org_id() OR auth.role() = 'service_role');

-- 14. Suppliers Policy
DROP POLICY IF EXISTS org_isolation_suppliers ON suppliers;
CREATE POLICY org_isolation_suppliers ON suppliers
    FOR ALL
    USING (organization_id = current_org_id() OR auth.role() = 'service_role');

-- 15. Purchases Policy
DROP POLICY IF EXISTS org_isolation_purchases ON purchases;
CREATE POLICY org_isolation_purchases ON purchases
    FOR ALL
    USING (organization_id = current_org_id() OR auth.role() = 'service_role');

-- 16. Sales Policy
DROP POLICY IF EXISTS org_isolation_sales ON sales;
CREATE POLICY org_isolation_sales ON sales
    FOR ALL
    USING (organization_id = current_org_id() OR auth.role() = 'service_role');

-- 17. Invoices Policy
DROP POLICY IF EXISTS org_isolation_invoices ON invoices;
CREATE POLICY org_isolation_invoices ON invoices
    FOR ALL
    USING (organization_id = current_org_id() OR auth.role() = 'service_role');

-- 18. Payments Policy
DROP POLICY IF EXISTS org_isolation_payments ON payments;
CREATE POLICY org_isolation_payments ON payments
    FOR ALL
    USING (organization_id = current_org_id() OR auth.role() = 'service_role');

-- 19. Sync Queue Policy
DROP POLICY IF EXISTS org_isolation_sync_queue ON sync_queue;
CREATE POLICY org_isolation_sync_queue ON sync_queue
    FOR ALL
    USING (organization_id = current_org_id() OR auth.role() = 'service_role');

-- 20. Applied Sync Keys Policy
DROP POLICY IF EXISTS org_isolation_applied_sync_keys ON applied_sync_keys;
CREATE POLICY org_isolation_applied_sync_keys ON applied_sync_keys
    FOR ALL
    USING (organization_id = current_org_id() OR auth.role() = 'service_role');

-- 21. Audit Logs Policy
DROP POLICY IF EXISTS org_isolation_audit_logs ON audit_logs;
CREATE POLICY org_isolation_audit_logs ON audit_logs
    FOR ALL
    USING (organization_id = current_org_id() OR auth.role() = 'service_role');
