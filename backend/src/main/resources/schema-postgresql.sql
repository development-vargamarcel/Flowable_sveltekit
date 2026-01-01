-- Business Tables Schema for Variable Storage (PostgreSQL)
-- This file is executed on application startup when using PostgreSQL profile

-- ============================================
-- 1. DOCUMENT TABLE - One record per process
-- ============================================
CREATE TABLE IF NOT EXISTS document (
    id BIGSERIAL PRIMARY KEY,

    -- Process linking (both methods)
    process_instance_id VARCHAR(64) NOT NULL,
    business_key VARCHAR(255),

    -- Process metadata
    process_definition_key VARCHAR(255),
    process_definition_name VARCHAR(255),

    -- 30 VARCHAR columns (TEXT in PostgreSQL for flexibility)
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

    -- 30 FLOAT columns (DOUBLE PRECISION in PostgreSQL)
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
    datetime_1 TIMESTAMP,
    datetime_2 TIMESTAMP,
    datetime_3 TIMESTAMP,
    datetime_4 TIMESTAMP,
    datetime_5 TIMESTAMP,
    datetime_6 TIMESTAMP,
    datetime_7 TIMESTAMP,
    datetime_8 TIMESTAMP,
    datetime_9 TIMESTAMP,
    datetime_10 TIMESTAMP,
    datetime_11 TIMESTAMP,
    datetime_12 TIMESTAMP,
    datetime_13 TIMESTAMP,
    datetime_14 TIMESTAMP,
    datetime_15 TIMESTAMP,
    datetime_16 TIMESTAMP,
    datetime_17 TIMESTAMP,
    datetime_18 TIMESTAMP,
    datetime_19 TIMESTAMP,
    datetime_20 TIMESTAMP,
    datetime_21 TIMESTAMP,
    datetime_22 TIMESTAMP,
    datetime_23 TIMESTAMP,
    datetime_24 TIMESTAMP,
    datetime_25 TIMESTAMP,
    datetime_26 TIMESTAMP,
    datetime_27 TIMESTAMP,
    datetime_28 TIMESTAMP,
    datetime_29 TIMESTAMP,
    datetime_30 TIMESTAMP,

    -- Audit columns
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),

    -- Unique constraint on process instance
    CONSTRAINT uk_document_process_instance UNIQUE (process_instance_id)
);

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
    datetime_1 TIMESTAMP,
    datetime_2 TIMESTAMP,
    datetime_3 TIMESTAMP,
    datetime_4 TIMESTAMP,
    datetime_5 TIMESTAMP,
    datetime_6 TIMESTAMP,
    datetime_7 TIMESTAMP,
    datetime_8 TIMESTAMP,
    datetime_9 TIMESTAMP,
    datetime_10 TIMESTAMP,
    datetime_11 TIMESTAMP,
    datetime_12 TIMESTAMP,
    datetime_13 TIMESTAMP,
    datetime_14 TIMESTAMP,
    datetime_15 TIMESTAMP,
    datetime_16 TIMESTAMP,
    datetime_17 TIMESTAMP,
    datetime_18 TIMESTAMP,
    datetime_19 TIMESTAMP,
    datetime_20 TIMESTAMP,
    datetime_21 TIMESTAMP,
    datetime_22 TIMESTAMP,
    datetime_23 TIMESTAMP,
    datetime_24 TIMESTAMP,
    datetime_25 TIMESTAMP,
    datetime_26 TIMESTAMP,
    datetime_27 TIMESTAMP,
    datetime_28 TIMESTAMP,
    datetime_29 TIMESTAMP,
    datetime_30 TIMESTAMP,

    -- Audit columns
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

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
    grid_name VARCHAR(255),                    -- NULL for document fields

    -- Field info
    field_name VARCHAR(255) NOT NULL,
    field_type VARCHAR(20) NOT NULL,           -- 'VARCHAR', 'FLOAT', or 'DATETIME'

    -- Assigned column
    column_name VARCHAR(20) NOT NULL,          -- e.g., 'varchar_7', 'float_12', 'datetime_3'

    -- Metadata
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Unique constraint: one mapping per field per scope
    CONSTRAINT uk_column_mapping UNIQUE (scope_type, process_definition_key, grid_name, field_name)
);

CREATE INDEX IF NOT EXISTS idx_column_mapping_process ON column_mapping(process_definition_key);
CREATE INDEX IF NOT EXISTS idx_column_mapping_lookup ON column_mapping(scope_type, process_definition_key, grid_name);


-- ============================================
-- 4. PROCESS_CONFIG TABLE - Per-process persistence settings
-- ============================================
CREATE TABLE IF NOT EXISTS process_config (
    id BIGSERIAL PRIMARY KEY,
    process_definition_key VARCHAR(255) NOT NULL UNIQUE,

    -- Persistence settings
    persist_on_task_complete BOOLEAN DEFAULT TRUE,
    persist_on_process_complete BOOLEAN DEFAULT TRUE,

    -- Audit columns
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_process_config_key ON process_config(process_definition_key);
