-- ==============================================================================
-- SwiftLab Inventory & Billing Platform - Seed Data
-- ==============================================================================

-- 1. Default Organization
INSERT INTO organizations (id, name, currency, country, tax_id, plan, active, created_at, updated_at)
VALUES (1, 'SwiftLab Demo Enterprise', 'USD', 'United States', 'TAX-US-99881', 'ENTERPRISE', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- 2. Initial Owner User (Username: admin, Password: admin123, Role: OWNER)
-- BCrypt hash for 'admin123'
INSERT INTO users (id, organization_id, username, password, full_name, email, organization_name, phone, role, active, created_at, updated_at)
VALUES (1, 1, 'admin', '$2a$10$wI5Qj56Ym3/aJ9b6Yw7z.OnrU9F8R4tE.nE5lSOmhIeXqQ2tNrkXq', 'System Administrator', 'admin@swiftlab.io', 'SwiftLab Demo Enterprise', '+1 (555) 019-2831', 'OWNER', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- 3. Company Settings
INSERT INTO company_settings (id, organization_id, user_id, company_name, tagline, address, phone, email, website, tax_id, invoice_prefix, default_tax_rate, currency_symbol, invoice_footer_notes, updated_at)
VALUES (1, 1, 1, 'SwiftLab Technologies Inc.', 'Modern Inventory & Invoicing Solutions', '100 Innovation Way, Suite 400, Tech City, CA 94016', '+1 (555) 019-2831', 'billing@swiftlab.io', 'https://swiftlab.io', 'TAX-US-99881', 'INV-', 8.25, '$', 'Thank you for choosing SwiftLab. Payment is due within 15 days of invoice date.', CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- 4. Default Warehouse
INSERT INTO warehouses (id, organization_id, name, code, address, is_default, active, created_at, updated_at)
VALUES (1, 1, 'Main Central Depot', 'WH-01', 'Bay Area Logistics Center #4', TRUE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- 5. Standard Units
INSERT INTO units (id, organization_id, name, symbol, created_at, updated_at)
VALUES 
    (1, 1, 'Pieces', 'PCS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 1, 'Kilograms', 'KG', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (3, 1, 'Boxes', 'BOX', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- 6. Standard Categories
INSERT INTO categories (id, organization_id, name, description, created_at, updated_at)
VALUES 
    (1, 1, 'Electronics & Hardware', 'Computers, accessories, and barcode scanners', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 1, 'Office Supplies', 'Packaging materials, printer rolls, and stationery', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- 7. Standard Brands
INSERT INTO brands (id, organization_id, name, description, created_at, updated_at)
VALUES 
    (1, 1, 'SwiftLab Pro', 'SwiftLab proprietary certified hardware', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 1, 'Universal OEM', 'Standard industrial consumables', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- 8. Demo Customers & Suppliers
INSERT INTO customers (id, organization_id, name, email, phone, address, tax_number, active, deleted, created_at, updated_at)
VALUES (1, 1, 'Acme Retailers Corp', 'purchasing@acmeretail.com', '+1 (555) 321-4455', '45 Market St, San Francisco, CA', 'US-RET-40912', TRUE, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

INSERT INTO suppliers (id, organization_id, name, email, phone, address, tax_number, active, deleted, created_at, updated_at)
VALUES (1, 1, 'Global Hardware Wholesalers Ltd', 'orders@globalhardware.com', '+1 (555) 789-1122', '800 Industrial Blvd, Chicago, IL', 'US-SUP-88219', TRUE, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- Reset sequence IDs for PostgreSQL
SELECT setval('organizations_id_seq', (SELECT COALESCE(MAX(id), 1) FROM organizations));
SELECT setval('users_id_seq', (SELECT COALESCE(MAX(id), 1) FROM users));
SELECT setval('company_settings_id_seq', (SELECT COALESCE(MAX(id), 1) FROM company_settings));
SELECT setval('warehouses_id_seq', (SELECT COALESCE(MAX(id), 1) FROM warehouses));
SELECT setval('units_id_seq', (SELECT COALESCE(MAX(id), 1) FROM units));
SELECT setval('categories_id_seq', (SELECT COALESCE(MAX(id), 1) FROM categories));
SELECT setval('brands_id_seq', (SELECT COALESCE(MAX(id), 1) FROM brands));
SELECT setval('customers_id_seq', (SELECT COALESCE(MAX(id), 1) FROM customers));
SELECT setval('suppliers_id_seq', (SELECT COALESCE(MAX(id), 1) FROM suppliers));
