# Plan: Business Tables for Variable Storage

## Overview

Create two database tables to store process variables in a structured, queryable format:
- **`document`**: One record per process instance (stores form fields)
- **`grid_rows`**: One record per grid row (stores grid data)

Both tables will have 60 columns (30 varchar, 30 float) plus metadata columns.

---

## Table Schemas

### 1. Document Table

```sql
CREATE TABLE IF NOT EXISTS document (
    -- Primary key
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    -- Process linking (both methods)
    process_instance_id VARCHAR(64) NOT NULL,
    business_key VARCHAR(255),

    -- Process metadata
    process_definition_key VARCHAR(255),
    process_definition_name VARCHAR(255),

    -- 30 VARCHAR columns (max length)
    varchar_1 VARCHAR(4000),
    varchar_2 VARCHAR(4000),
    varchar_3 VARCHAR(4000),
    varchar_4 VARCHAR(4000),
    varchar_5 VARCHAR(4000),
    varchar_6 VARCHAR(4000),
    varchar_7 VARCHAR(4000),
    varchar_8 VARCHAR(4000),
    varchar_9 VARCHAR(4000),
    varchar_10 VARCHAR(4000),
    varchar_11 VARCHAR(4000),
    varchar_12 VARCHAR(4000),
    varchar_13 VARCHAR(4000),
    varchar_14 VARCHAR(4000),
    varchar_15 VARCHAR(4000),
    varchar_16 VARCHAR(4000),
    varchar_17 VARCHAR(4000),
    varchar_18 VARCHAR(4000),
    varchar_19 VARCHAR(4000),
    varchar_20 VARCHAR(4000),
    varchar_21 VARCHAR(4000),
    varchar_22 VARCHAR(4000),
    varchar_23 VARCHAR(4000),
    varchar_24 VARCHAR(4000),
    varchar_25 VARCHAR(4000),
    varchar_26 VARCHAR(4000),
    varchar_27 VARCHAR(4000),
    varchar_28 VARCHAR(4000),
    varchar_29 VARCHAR(4000),
    varchar_30 VARCHAR(4000),

    -- 30 FLOAT columns (can represent boolean as 0/1)
    float_1 DOUBLE,
    float_2 DOUBLE,
    float_3 DOUBLE,
    float_4 DOUBLE,
    float_5 DOUBLE,
    float_6 DOUBLE,
    float_7 DOUBLE,
    float_8 DOUBLE,
    float_9 DOUBLE,
    float_10 DOUBLE,
    float_11 DOUBLE,
    float_12 DOUBLE,
    float_13 DOUBLE,
    float_14 DOUBLE,
    float_15 DOUBLE,
    float_16 DOUBLE,
    float_17 DOUBLE,
    float_18 DOUBLE,
    float_19 DOUBLE,
    float_20 DOUBLE,
    float_21 DOUBLE,
    float_22 DOUBLE,
    float_23 DOUBLE,
    float_24 DOUBLE,
    float_25 DOUBLE,
    float_26 DOUBLE,
    float_27 DOUBLE,
    float_28 DOUBLE,
    float_29 DOUBLE,
    float_30 DOUBLE,

    -- Audit columns
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),

    -- Indexes
    CONSTRAINT uk_document_process_instance UNIQUE (process_instance_id)
);

CREATE INDEX idx_document_business_key ON document(business_key);
CREATE INDEX idx_document_process_def_key ON document(process_definition_key);
```

### 2. Grid Rows Table

```sql
CREATE TABLE IF NOT EXISTS grid_rows (
    -- Primary key
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    -- Foreign key to document
    document_id BIGINT NOT NULL,

    -- Process linking (for direct queries)
    process_instance_id VARCHAR(64) NOT NULL,

    -- Grid identification
    grid_name VARCHAR(255) NOT NULL,
    row_index INT NOT NULL,

    -- 30 VARCHAR columns (max length)
    varchar_1 VARCHAR(4000),
    varchar_2 VARCHAR(4000),
    varchar_3 VARCHAR(4000),
    varchar_4 VARCHAR(4000),
    varchar_5 VARCHAR(4000),
    varchar_6 VARCHAR(4000),
    varchar_7 VARCHAR(4000),
    varchar_8 VARCHAR(4000),
    varchar_9 VARCHAR(4000),
    varchar_10 VARCHAR(4000),
    varchar_11 VARCHAR(4000),
    varchar_12 VARCHAR(4000),
    varchar_13 VARCHAR(4000),
    varchar_14 VARCHAR(4000),
    varchar_15 VARCHAR(4000),
    varchar_16 VARCHAR(4000),
    varchar_17 VARCHAR(4000),
    varchar_18 VARCHAR(4000),
    varchar_19 VARCHAR(4000),
    varchar_20 VARCHAR(4000),
    varchar_21 VARCHAR(4000),
    varchar_22 VARCHAR(4000),
    varchar_23 VARCHAR(4000),
    varchar_24 VARCHAR(4000),
    varchar_25 VARCHAR(4000),
    varchar_26 VARCHAR(4000),
    varchar_27 VARCHAR(4000),
    varchar_28 VARCHAR(4000),
    varchar_29 VARCHAR(4000),
    varchar_30 VARCHAR(4000),

    -- 30 FLOAT columns (can represent boolean as 0/1)
    float_1 DOUBLE,
    float_2 DOUBLE,
    float_3 DOUBLE,
    float_4 DOUBLE,
    float_5 DOUBLE,
    float_6 DOUBLE,
    float_7 DOUBLE,
    float_8 DOUBLE,
    float_9 DOUBLE,
    float_10 DOUBLE,
    float_11 DOUBLE,
    float_12 DOUBLE,
    float_13 DOUBLE,
    float_14 DOUBLE,
    float_15 DOUBLE,
    float_16 DOUBLE,
    float_17 DOUBLE,
    float_18 DOUBLE,
    float_19 DOUBLE,
    float_20 DOUBLE,
    float_21 DOUBLE,
    float_22 DOUBLE,
    float_23 DOUBLE,
    float_24 DOUBLE,
    float_25 DOUBLE,
    float_26 DOUBLE,
    float_27 DOUBLE,
    float_28 DOUBLE,
    float_29 DOUBLE,
    float_30 DOUBLE,

    -- Audit columns
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Foreign key constraint
    CONSTRAINT fk_grid_rows_document FOREIGN KEY (document_id) REFERENCES document(id) ON DELETE CASCADE
);

CREATE INDEX idx_grid_rows_process_instance ON grid_rows(process_instance_id);
CREATE INDEX idx_grid_rows_grid_name ON grid_rows(grid_name);
CREATE INDEX idx_grid_rows_document_grid ON grid_rows(document_id, grid_name);
```

---

## Implementation Steps

### Step 1: Database Configuration

**File:** `backend/src/main/resources/application.yml`

- Change H2 from in-memory to file-based mode for data persistence
- Update datasource URL: `jdbc:h2:file:./data/flowabledb;DB_CLOSE_DELAY=-1`

### Step 2: Create Schema Initialization Script

**File:** `backend/src/main/resources/schema.sql`

- Create SQL file with both table definitions
- Spring Boot will auto-execute on startup

### Step 3: Create Entity Classes

**Files to create:**
- `backend/src/main/java/com/demo/bpm/entity/Document.java`
- `backend/src/main/java/com/demo/bpm/entity/GridRow.java`

Using JPA annotations for ORM mapping.

### Step 4: Create Repository Interfaces

**Files to create:**
- `backend/src/main/java/com/demo/bpm/repository/DocumentRepository.java`
- `backend/src/main/java/com/demo/bpm/repository/GridRowRepository.java`

Spring Data JPA repositories with custom query methods.

### Step 5: Create DTO Classes

**Files to create:**
- `backend/src/main/java/com/demo/bpm/dto/DocumentDTO.java`
- `backend/src/main/java/com/demo/bpm/dto/GridRowDTO.java`

For API responses and data transfer.

### Step 6: Create Service Layer

**File:** `backend/src/main/java/com/demo/bpm/service/BusinessTableService.java`

Key methods:
```java
// Document operations
Document createDocument(String processInstanceId, String businessKey, Map<String, Object> variables);
Document updateDocument(String processInstanceId, Map<String, Object> variables);
Document getDocumentByProcessInstanceId(String processInstanceId);
Document getDocumentByBusinessKey(String businessKey);

// Grid row operations
List<GridRow> saveGridRows(Long documentId, String gridName, List<Map<String, Object>> rows);
List<GridRow> getGridRows(Long documentId, String gridName);
void deleteGridRows(Long documentId, String gridName);

// Field mapping
void setFieldMapping(String processDefinitionKey, Map<String, ColumnMapping> mappings);
```

### Step 7: Create Column Mapping Configuration

**File:** `backend/src/main/java/com/demo/bpm/config/ColumnMappingConfig.java`

Define how form fields map to table columns:
```java
public class ColumnMapping {
    private String fieldName;      // e.g., "customerName"
    private String columnName;     // e.g., "varchar_1"
    private ColumnType type;       // VARCHAR or FLOAT
}
```

**File:** `backend/src/main/resources/column-mappings.yml` (optional)

Allow configurable field-to-column mappings per process definition.

### Step 8: Create REST Controller

**File:** `backend/src/main/java/com/demo/bpm/controller/BusinessTableController.java`

Endpoints:
```
GET    /api/documents/{processInstanceId}
GET    /api/documents/by-business-key/{businessKey}
POST   /api/documents
PUT    /api/documents/{processInstanceId}

GET    /api/documents/{documentId}/grids/{gridName}
POST   /api/documents/{documentId}/grids/{gridName}
DELETE /api/documents/{documentId}/grids/{gridName}
```

### Step 9: Integrate with Process Flow

**Modify:** `backend/src/main/java/com/demo/bpm/service/TaskService.java`

When a task is completed:
1. Extract form values and grid data
2. Create/update document record
3. Save grid rows with proper mappings

### Step 10: Add JPA Dependencies

**File:** `backend/pom.xml`

Add Spring Data JPA dependency if not already present.

---

## Column Mapping Strategy: Hash-Based with Database Storage

### Chosen Approach: Hash + DB + Collision Resolution

1. **Hash-based initial assignment**: Field name hashed to determine preferred column
2. **Database storage**: Actual mappings persisted in `column_mapping` table
3. **Collision resolution**: If column taken, find nearest available column

### Column Mapping Table

```sql
CREATE TABLE IF NOT EXISTS column_mapping (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    -- Scope: process-level for document fields, grid-level for grid columns
    scope_type VARCHAR(20) NOT NULL,           -- 'DOCUMENT' or 'GRID'
    process_definition_key VARCHAR(255) NOT NULL,
    grid_name VARCHAR(255),                    -- NULL for document fields

    -- Field info
    field_name VARCHAR(255) NOT NULL,
    field_type VARCHAR(20) NOT NULL,           -- 'VARCHAR' or 'FLOAT'

    -- Assigned column
    column_name VARCHAR(20) NOT NULL,          -- e.g., 'varchar_7', 'float_12'

    -- Metadata
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Unique constraint: one mapping per field per scope
    CONSTRAINT uk_column_mapping UNIQUE (scope_type, process_definition_key, grid_name, field_name)
);

CREATE INDEX idx_column_mapping_process ON column_mapping(process_definition_key);
CREATE INDEX idx_column_mapping_lookup ON column_mapping(scope_type, process_definition_key, grid_name);
```

### Mapping Algorithm

```java
public class HashBasedColumnMapper {

    private static final int MAX_COLUMNS = 30;

    /**
     * Get or create column mapping for a field.
     * 1. Check DB for existing mapping → return if found
     * 2. Calculate hash-based preferred column
     * 3. If taken, find nearest available
     * 4. Store mapping in DB and return
     */
    public String getColumnName(String processDefKey, String gridName,
                                 String fieldName, FieldType type) {

        // 1. Check existing mapping
        Optional<ColumnMapping> existing = repository.findMapping(
            processDefKey, gridName, fieldName);
        if (existing.isPresent()) {
            return existing.get().getColumnName();
        }

        // 2. Calculate preferred column from hash
        int preferredColumn = calculatePreferredColumn(fieldName);
        String prefix = (type == FieldType.VARCHAR) ? "varchar_" : "float_";

        // 3. Get all used columns for this scope
        Set<Integer> usedColumns = repository.getUsedColumns(
            processDefKey, gridName, type);

        // 4. Find available column (nearest to preferred)
        int assignedColumn = findNearestAvailable(preferredColumn, usedColumns);

        // 5. Store and return
        String columnName = prefix + assignedColumn;
        repository.saveMapping(processDefKey, gridName, fieldName, type, columnName);

        return columnName;
    }

    private int calculatePreferredColumn(String fieldName) {
        // Consistent hash: same field name always gets same preferred column
        int hash = Math.abs(fieldName.hashCode());
        return (hash % MAX_COLUMNS) + 1;  // 1-30
    }

    private int findNearestAvailable(int preferred, Set<Integer> used) {
        if (!used.contains(preferred)) {
            return preferred;
        }

        // Search outward from preferred: +1, -1, +2, -2, ...
        for (int offset = 1; offset < MAX_COLUMNS; offset++) {
            int higher = preferred + offset;
            int lower = preferred - offset;

            if (higher <= MAX_COLUMNS && !used.contains(higher)) {
                return higher;
            }
            if (lower >= 1 && !used.contains(lower)) {
                return lower;
            }
        }

        throw new RuntimeException("No available columns - all 30 are used");
    }
}
```

### Example: Collision Resolution

```
Process: expense-approval
Fields: customerName, category, comments (all VARCHAR)

Step 1: customerName
  hash("customerName") % 30 + 1 = 7
  varchar_7 available → assign varchar_7 ✓

Step 2: category
  hash("category") % 30 + 1 = 7  (collision!)
  varchar_7 taken → check varchar_8 → available → assign varchar_8 ✓

Step 3: comments
  hash("comments") % 30 + 1 = 8  (collision!)
  varchar_8 taken → check varchar_9 → available → assign varchar_9 ✓

Result stored in column_mapping table:
┌─────────────────────┬────────────────┬─────────────┐
│ field_name          │ field_type     │ column_name │
├─────────────────────┼────────────────┼─────────────┤
│ customerName        │ VARCHAR        │ varchar_7   │
│ category            │ VARCHAR        │ varchar_8   │
│ comments            │ VARCHAR        │ varchar_9   │
└─────────────────────┴────────────────┴─────────────┘
```

### Benefits of This Approach

| Benefit | Description |
|---------|-------------|
| **Zero config** | No YAML files to maintain |
| **Stable** | Once assigned, mapping never changes |
| **Queryable** | Can lookup mappings via SQL |
| **Deterministic** | Same field prefers same column across processes |
| **Collision-proof** | Graceful handling when hash collides |
| **Self-documenting** | `column_mapping` table shows all assignments |

---

## Data Flow Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    User Submits Form                        │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│              TaskController.completeTask()                  │
│   - Receives form values + grid data as JSON                │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                TaskService.completeTask()                   │
│   1. Complete Flowable task with variables                  │
│   2. Call BusinessTableService to persist                   │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│             BusinessTableService.saveData()                 │
│   1. Get/create Document for process instance               │
│   2. Map field values to varchar_X / float_X columns        │
│   3. Save document record                                   │
│   4. Delete existing grid rows for this submission          │
│   5. Map grid row values to columns                         │
│   6. Insert new grid_rows records                           │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                     H2 Database                             │
│   ┌─────────────────┐    ┌─────────────────┐               │
│   │    document     │◄───│   grid_rows     │               │
│   │ (1 per process) │    │ (N per grid)    │               │
│   └─────────────────┘    └─────────────────┘               │
└─────────────────────────────────────────────────────────────┘
```

---

## File Structure (New Files)

```
backend/src/main/
├── java/com/demo/bpm/
│   ├── entity/
│   │   ├── Document.java           # JPA entity for document table
│   │   ├── GridRow.java            # JPA entity for grid_rows table
│   │   └── ColumnMapping.java      # JPA entity for column_mapping table
│   ├── repository/
│   │   ├── DocumentRepository.java     # Spring Data JPA
│   │   ├── GridRowRepository.java      # Spring Data JPA
│   │   └── ColumnMappingRepository.java # Spring Data JPA
│   ├── dto/
│   │   ├── DocumentDTO.java        # Response DTO
│   │   └── GridRowDTO.java         # Response DTO
│   ├── service/
│   │   ├── BusinessTableService.java   # Main service for document/grid ops
│   │   └── ColumnMappingService.java   # Hash-based mapping logic
│   └── controller/
│       └── BusinessTableController.java
└── resources/
    └── schema.sql                  # Table creation DDL (3 tables)
```

---

## Decisions Made

| Question | Decision |
|----------|----------|
| **Column Mapping** | Hash-based + DB storage + collision resolution |
| **Column Split** | 30 varchar + 30 float per table |
| **Column Naming** | `varchar_1` ... `varchar_30`, `float_1` ... `float_30` |
| **Process Linking** | Both `process_instance_id` and `business_key` |
| **Grid Row Links** | FK to document + process_instance_id + grid_name |
| **Database** | H2 (file-based for persistence) |
| **When to Persist** | Configurable per process/task |
| **H2 File Location** | `./data/flowabledb` (relative to backend) |

## Configurable Persistence

Since persistence timing is configurable per process/task, we need a way to specify when to save:

### Option 1: BPMN Extension Attribute

Add custom attribute to task/process in BPMN XML:

```xml
<!-- Process-level default -->
<process id="expense-approval" flowable:persistToBusinessTable="ON_TASK_COMPLETE">

<!-- Task-level override -->
<userTask id="reviewTask" flowable:persistToBusinessTable="SKIP">
```

**Values:**
- `ON_TASK_COMPLETE` - Save after each task (default)
- `ON_PROCESS_COMPLETE` - Save only when process ends
- `SKIP` - Don't save for this task (inherits process setting)

### Option 2: Task Listener Approach

Register a task listener that checks configuration and persists:

```java
public class BusinessTableTaskListener implements TaskListener {
    @Override
    public void notify(DelegateTask task) {
        String setting = getProcessAttribute(task, "persistToBusinessTable");
        if ("ON_TASK_COMPLETE".equals(setting)) {
            businessTableService.persist(task);
        }
    }
}
```

### Recommended: Option 1 + Process-Level Config Table

Store process-level configuration in a new table:

```sql
CREATE TABLE IF NOT EXISTS process_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    process_definition_key VARCHAR(255) NOT NULL UNIQUE,
    persist_on_task_complete BOOLEAN DEFAULT TRUE,
    persist_on_process_complete BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

This allows runtime configuration without redeploying BPMN files

---

## Estimated Effort

| Task | Complexity |
|------|------------|
| Database configuration | Low |
| Schema creation | Low |
| Entity classes | Medium |
| Repository interfaces | Low |
| Service layer | High |
| Column mapping | Medium |
| REST controller | Medium |
| Process integration | High |
| Testing | Medium |

---

## Risks and Considerations

1. **Column Limit**: 60 columns may not be enough for complex processes
   - Mitigation: Can add more columns later or use JSON column for overflow

2. **Type Mismatch**: Storing dates as varchar requires consistent formatting
   - Mitigation: Use ISO-8601 format consistently

3. **Performance**: Large grid data may impact performance
   - Mitigation: Add proper indexes, consider batch inserts

4. **Data Consistency**: Business tables must stay in sync with Flowable variables
   - Mitigation: Use database transactions, handle failures gracefully
