-- Business Tables Schema for PostgreSQL
-- This file is executed on application startup when the 'postgresql' profile is active

-- ============================================
-- 1. DOCUMENT TABLE - Multiple documents per process (one per type)
-- ============================================
CREATE TABLE IF NOT EXISTS document (
    id BIGSERIAL PRIMARY KEY,

    -- Process linking (both methods)
    process_instance_id VARCHAR(64) NOT NULL,
    business_key VARCHAR(255),

    -- Process metadata
    process_definition_key VARCHAR(255),
    process_definition_name VARCHAR(255),

    -- Document type (allows multiple documents per process)
    type VARCHAR(100) NOT NULL DEFAULT 'main',

    -- 30 VARCHAR columns
    varchar_1 TEXT,
    varchar_2 TEXT,
    varchar_3 TEXT,
    varchar_4 TEXT,
    varchar_5 TEXT,
    varchar_6 TEXT,
    varchar_7 TEXT,
    varchar_8 TEXT,
    varchar_9 TEXT,
    varchar_10 TEXT,
    varchar_11 TEXT,
    varchar_12 TEXT,
    varchar_13 TEXT,
    varchar_14 TEXT,
    varchar_15 TEXT,
    varchar_16 TEXT,
    varchar_17 TEXT,
    varchar_18 TEXT,
    varchar_19 TEXT,
    varchar_20 TEXT,
    varchar_21 TEXT,
    varchar_22 TEXT,
    varchar_23 TEXT,
    varchar_24 TEXT,
    varchar_25 TEXT,
    varchar_26 TEXT,
    varchar_27 TEXT,
    varchar_28 TEXT,
    varchar_29 TEXT,
    varchar_30 TEXT,

    -- 30 FLOAT columns (DOUBLE PRECISION for precision)
    float_1 DOUBLE PRECISION,
    float_2 DOUBLE PRECISION,
    float_3 DOUBLE PRECISION,
    float_4 DOUBLE PRECISION,
    float_5 DOUBLE PRECISION,
    float_6 DOUBLE PRECISION,
    float_7 DOUBLE PRECISION,
    float_8 DOUBLE PRECISION,
    float_9 DOUBLE PRECISION,
    float_10 DOUBLE PRECISION,
    float_11 DOUBLE PRECISION,
    float_12 DOUBLE PRECISION,
    float_13 DOUBLE PRECISION,
    float_14 DOUBLE PRECISION,
    float_15 DOUBLE PRECISION,
    float_16 DOUBLE PRECISION,
    float_17 DOUBLE PRECISION,
    float_18 DOUBLE PRECISION,
    float_19 DOUBLE PRECISION,
    float_20 DOUBLE PRECISION,
    float_21 DOUBLE PRECISION,
    float_22 DOUBLE PRECISION,
    float_23 DOUBLE PRECISION,
    float_24 DOUBLE PRECISION,
    float_25 DOUBLE PRECISION,
    float_26 DOUBLE PRECISION,
    float_27 DOUBLE PRECISION,
    float_28 DOUBLE PRECISION,
    float_29 DOUBLE PRECISION,
    float_30 DOUBLE PRECISION,

    -- 30 DATETIME columns
    datetime_1 TIMESTAMP WITH TIME ZONE,
    datetime_2 TIMESTAMP WITH TIME ZONE,
    datetime_3 TIMESTAMP WITH TIME ZONE,
    datetime_4 TIMESTAMP WITH TIME ZONE,
    datetime_5 TIMESTAMP WITH TIME ZONE,
    datetime_6 TIMESTAMP WITH TIME ZONE,
    datetime_7 TIMESTAMP WITH TIME ZONE,
    datetime_8 TIMESTAMP WITH TIME ZONE,
    datetime_9 TIMESTAMP WITH TIME ZONE,
    datetime_10 TIMESTAMP WITH TIME ZONE,
    datetime_11 TIMESTAMP WITH TIME ZONE,
    datetime_12 TIMESTAMP WITH TIME ZONE,
    datetime_13 TIMESTAMP WITH TIME ZONE,
    datetime_14 TIMESTAMP WITH TIME ZONE,
    datetime_15 TIMESTAMP WITH TIME ZONE,
    datetime_16 TIMESTAMP WITH TIME ZONE,
    datetime_17 TIMESTAMP WITH TIME ZONE,
    datetime_18 TIMESTAMP WITH TIME ZONE,
    datetime_19 TIMESTAMP WITH TIME ZONE,
    datetime_20 TIMESTAMP WITH TIME ZONE,
    datetime_21 TIMESTAMP WITH TIME ZONE,
    datetime_22 TIMESTAMP WITH TIME ZONE,
    datetime_23 TIMESTAMP WITH TIME ZONE,
    datetime_24 TIMESTAMP WITH TIME ZONE,
    datetime_25 TIMESTAMP WITH TIME ZONE,
    datetime_26 TIMESTAMP WITH TIME ZONE,
    datetime_27 TIMESTAMP WITH TIME ZONE,
    datetime_28 TIMESTAMP WITH TIME ZONE,
    datetime_29 TIMESTAMP WITH TIME ZONE,
    datetime_30 TIMESTAMP WITH TIME ZONE,

    -- Audit columns
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),

    -- Unique constraint: one document per type per process
    CONSTRAINT uk_document_process_type UNIQUE (process_instance_id, type)
);

CREATE INDEX IF NOT EXISTS idx_document_process_instance ON document(process_instance_id);
CREATE INDEX IF NOT EXISTS idx_document_type ON document(type);
CREATE INDEX IF NOT EXISTS idx_document_business_key ON document(business_key);
CREATE INDEX IF NOT EXISTS idx_document_process_def_key ON document(process_definition_key);


-- ============================================
-- 2. GRID_ROWS TABLE - One record per grid row
-- ============================================
CREATE TABLE IF NOT EXISTS grid_rows (
    id BIGSERIAL PRIMARY KEY,

    -- Foreign key to document
    document_id BIGINT NOT NULL,

    -- Process linking (for direct queries)
    process_instance_id VARCHAR(64) NOT NULL,

    -- Grid identification
    grid_name VARCHAR(255) NOT NULL,
    row_index INT NOT NULL,

    -- 30 VARCHAR columns
    varchar_1 TEXT,
    varchar_2 TEXT,
    varchar_3 TEXT,
    varchar_4 TEXT,
    varchar_5 TEXT,
    varchar_6 TEXT,
    varchar_7 TEXT,
    varchar_8 TEXT,
    varchar_9 TEXT,
    varchar_10 TEXT,
    varchar_11 TEXT,
    varchar_12 TEXT,
    varchar_13 TEXT,
    varchar_14 TEXT,
    varchar_15 TEXT,
    varchar_16 TEXT,
    varchar_17 TEXT,
    varchar_18 TEXT,
    varchar_19 TEXT,
    varchar_20 TEXT,
    varchar_21 TEXT,
    varchar_22 TEXT,
    varchar_23 TEXT,
    varchar_24 TEXT,
    varchar_25 TEXT,
    varchar_26 TEXT,
    varchar_27 TEXT,
    varchar_28 TEXT,
    varchar_29 TEXT,
    varchar_30 TEXT,

    -- 30 FLOAT columns
    float_1 DOUBLE PRECISION,
    float_2 DOUBLE PRECISION,
    float_3 DOUBLE PRECISION,
    float_4 DOUBLE PRECISION,
    float_5 DOUBLE PRECISION,
    float_6 DOUBLE PRECISION,
    float_7 DOUBLE PRECISION,
    float_8 DOUBLE PRECISION,
    float_9 DOUBLE PRECISION,
    float_10 DOUBLE PRECISION,
    float_11 DOUBLE PRECISION,
    float_12 DOUBLE PRECISION,
    float_13 DOUBLE PRECISION,
    float_14 DOUBLE PRECISION,
    float_15 DOUBLE PRECISION,
    float_16 DOUBLE PRECISION,
    float_17 DOUBLE PRECISION,
    float_18 DOUBLE PRECISION,
    float_19 DOUBLE PRECISION,
    float_20 DOUBLE PRECISION,
    float_21 DOUBLE PRECISION,
    float_22 DOUBLE PRECISION,
    float_23 DOUBLE PRECISION,
    float_24 DOUBLE PRECISION,
    float_25 DOUBLE PRECISION,
    float_26 DOUBLE PRECISION,
    float_27 DOUBLE PRECISION,
    float_28 DOUBLE PRECISION,
    float_29 DOUBLE PRECISION,
    float_30 DOUBLE PRECISION,

    -- 30 DATETIME columns
    datetime_1 TIMESTAMP WITH TIME ZONE,
    datetime_2 TIMESTAMP WITH TIME ZONE,
    datetime_3 TIMESTAMP WITH TIME ZONE,
    datetime_4 TIMESTAMP WITH TIME ZONE,
    datetime_5 TIMESTAMP WITH TIME ZONE,
    datetime_6 TIMESTAMP WITH TIME ZONE,
    datetime_7 TIMESTAMP WITH TIME ZONE,
    datetime_8 TIMESTAMP WITH TIME ZONE,
    datetime_9 TIMESTAMP WITH TIME ZONE,
    datetime_10 TIMESTAMP WITH TIME ZONE,
    datetime_11 TIMESTAMP WITH TIME ZONE,
    datetime_12 TIMESTAMP WITH TIME ZONE,
    datetime_13 TIMESTAMP WITH TIME ZONE,
    datetime_14 TIMESTAMP WITH TIME ZONE,
    datetime_15 TIMESTAMP WITH TIME ZONE,
    datetime_16 TIMESTAMP WITH TIME ZONE,
    datetime_17 TIMESTAMP WITH TIME ZONE,
    datetime_18 TIMESTAMP WITH TIME ZONE,
    datetime_19 TIMESTAMP WITH TIME ZONE,
    datetime_20 TIMESTAMP WITH TIME ZONE,
    datetime_21 TIMESTAMP WITH TIME ZONE,
    datetime_22 TIMESTAMP WITH TIME ZONE,
    datetime_23 TIMESTAMP WITH TIME ZONE,
    datetime_24 TIMESTAMP WITH TIME ZONE,
    datetime_25 TIMESTAMP WITH TIME ZONE,
    datetime_26 TIMESTAMP WITH TIME ZONE,
    datetime_27 TIMESTAMP WITH TIME ZONE,
    datetime_28 TIMESTAMP WITH TIME ZONE,
    datetime_29 TIMESTAMP WITH TIME ZONE,
    datetime_30 TIMESTAMP WITH TIME ZONE,

    -- Audit columns
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    -- Foreign key constraint
    CONSTRAINT fk_grid_rows_document FOREIGN KEY (document_id) REFERENCES document(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_grid_rows_process_instance ON grid_rows(process_instance_id);
CREATE INDEX IF NOT EXISTS idx_grid_rows_grid_name ON grid_rows(grid_name);
CREATE INDEX IF NOT EXISTS idx_grid_rows_document_grid ON grid_rows(document_id, grid_name);


-- ============================================
-- 3. COLUMN_MAPPING TABLE - Field to column assignments
-- ============================================
CREATE TABLE IF NOT EXISTS column_mapping (
    id BIGSERIAL PRIMARY KEY,

    -- Scope: process-level for document fields, grid-level for grid columns
    scope_type VARCHAR(20) NOT NULL,           -- 'DOCUMENT' or 'GRID'
    process_definition_key VARCHAR(255) NOT NULL,
    document_type VARCHAR(100),                -- Document type for document-specific mappings
    grid_name VARCHAR(255),                    -- NULL for document fields

    -- Field info
    field_name VARCHAR(255) NOT NULL,
    field_type VARCHAR(20) NOT NULL,           -- 'VARCHAR', 'FLOAT', or 'DATETIME'

    -- Assigned column
    column_name VARCHAR(20) NOT NULL,          -- e.g., 'varchar_7', 'float_12', 'datetime_3'

    -- Metadata
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    -- Unique constraint: one mapping per field per scope (includes document_type)
    CONSTRAINT uk_column_mapping UNIQUE (scope_type, process_definition_key, document_type, grid_name, field_name)
);

CREATE INDEX IF NOT EXISTS idx_column_mapping_process ON column_mapping(process_definition_key);
CREATE INDEX IF NOT EXISTS idx_column_mapping_document_type ON column_mapping(document_type);
CREATE INDEX IF NOT EXISTS idx_column_mapping_lookup ON column_mapping(scope_type, process_definition_key, document_type, grid_name);


-- ============================================
-- 4. PROCESS_CONFIG TABLE - Per-process persistence settings
-- ============================================
CREATE TABLE IF NOT EXISTS process_config (
    id BIGSERIAL PRIMARY KEY,
    process_definition_key VARCHAR(255) NOT NULL UNIQUE,

    -- Persistence settings
    persist_on_task_complete BOOLEAN DEFAULT TRUE,
    persist_on_process_complete BOOLEAN DEFAULT TRUE,

    document_type VARCHAR(100),

    -- Audit columns
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_process_config_key ON process_config(process_definition_key);


-- ============================================
-- 5. DOCUMENT_TYPE_DEFINITION TABLE - Definitions for reusable document types
-- ============================================
CREATE TABLE IF NOT EXISTS document_type_definition (
    id BIGSERIAL PRIMARY KEY,
    key_id VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    schema_json TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_document_type_def_key ON document_type_definition(key_id);


-- ============================================
-- 6. DEMO TABLES - For showcasing SQL Logic features
-- ============================================

CREATE TABLE IF NOT EXISTS DEMO_CUSTOMERS (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    segment VARCHAR(50),
    credit_limit DECIMAL(10, 2)
);

CREATE TABLE IF NOT EXISTS DEMO_PRODUCTS (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(50),
    price DECIMAL(10, 2),
    stock INT
);

-- Seed Data for DEMO_CUSTOMERS
INSERT INTO DEMO_CUSTOMERS (name, email, segment, credit_limit)
SELECT 'Acme Corp', 'contact@acme.com', 'Enterprise', 50000.00
WHERE NOT EXISTS (SELECT 1 FROM DEMO_CUSTOMERS WHERE name = 'Acme Corp');

INSERT INTO DEMO_CUSTOMERS (name, email, segment, credit_limit)
SELECT 'Global Tech', 'info@globaltech.com', 'SMB', 10000.00
WHERE NOT EXISTS (SELECT 1 FROM DEMO_CUSTOMERS WHERE name = 'Global Tech');

INSERT INTO DEMO_CUSTOMERS (name, email, segment, credit_limit)
SELECT 'John Doe', 'john@example.com', 'Individual', 1000.00
WHERE NOT EXISTS (SELECT 1 FROM DEMO_CUSTOMERS WHERE name = 'John Doe');

-- Seed Data for DEMO_PRODUCTS
INSERT INTO DEMO_PRODUCTS (name, category, price, stock)
SELECT 'Laptop Pro', 'Electronics', 1299.99, 50
WHERE NOT EXISTS (SELECT 1 FROM DEMO_PRODUCTS WHERE name = 'Laptop Pro');

INSERT INTO DEMO_PRODUCTS (name, category, price, stock)
SELECT 'Ergo Chair', 'Furniture', 299.99, 100
WHERE NOT EXISTS (SELECT 1 FROM DEMO_PRODUCTS WHERE name = 'Ergo Chair');

INSERT INTO DEMO_PRODUCTS (name, category, price, stock)
SELECT 'Wireless Mouse', 'Electronics', 29.99, 200
WHERE NOT EXISTS (SELECT 1 FROM DEMO_PRODUCTS WHERE name = 'Wireless Mouse');


-- ============================================
-- 7. APP_PERMISSIONS TABLE - Permission definitions
-- ============================================
CREATE TABLE IF NOT EXISTS app_permissions (
    name VARCHAR(255) PRIMARY KEY,
    description VARCHAR(1000)
);

-- ============================================
-- 8. APP_ROLES TABLE - Role definitions
-- ============================================
CREATE TABLE IF NOT EXISTS app_roles (
    name VARCHAR(255) PRIMARY KEY,
    description VARCHAR(1000)
);

-- ============================================
-- 9. APP_ROLE_PERMISSIONS TABLE - Role-Permission mapping (join table)
-- ============================================
CREATE TABLE IF NOT EXISTS app_role_permissions (
    role_name VARCHAR(255) NOT NULL,
    permission_name VARCHAR(255) NOT NULL,
    PRIMARY KEY (role_name, permission_name),
    CONSTRAINT fk_role_permission_role FOREIGN KEY (role_name) REFERENCES app_roles(name) ON DELETE CASCADE,
    CONSTRAINT fk_role_permission_perm FOREIGN KEY (permission_name) REFERENCES app_permissions(name) ON DELETE CASCADE
);
