package com.demo.bpm.service;

import com.demo.bpm.entity.ColumnMapping;
import com.demo.bpm.entity.ColumnMapping.FieldType;
import com.demo.bpm.entity.ColumnMapping.ScopeType;
import com.demo.bpm.repository.ColumnMappingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Service for managing field-to-column mappings using hash-based assignment
 * with collision resolution.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ColumnMappingService {

    private static final int MAX_COLUMNS = 30;

    private final ColumnMappingRepository columnMappingRepository;

    /**
     * Get or create column mapping for a document field.
     */
    @Transactional
    public ColumnMapping getOrCreateDocumentMapping(String processDefKey, String fieldName, FieldType fieldType) {
        // 1. Check for existing mapping
        Optional<ColumnMapping> existing = columnMappingRepository.findDocumentFieldMapping(processDefKey, fieldName);
        if (existing.isPresent()) {
            return existing.get();
        }

        // 2. Calculate preferred column from hash
        int preferredColumn = calculatePreferredColumn(fieldName);

        // 3. Get used columns for this scope and type
        Set<Integer> usedColumns = columnMappingRepository.findUsedDocumentColumnIndices(processDefKey, fieldType);

        // 4. Find available column (nearest to preferred)
        int assignedColumn = findNearestAvailable(preferredColumn, usedColumns);

        // 5. Create and save mapping
        String columnName = (fieldType == FieldType.VARCHAR ? "varchar_" : "float_") + assignedColumn;

        ColumnMapping mapping = ColumnMapping.builder()
                .scopeType(ScopeType.DOCUMENT)
                .processDefinitionKey(processDefKey)
                .gridName(null)
                .fieldName(fieldName)
                .fieldType(fieldType)
                .columnName(columnName)
                .build();

        mapping = columnMappingRepository.save(mapping);
        log.info("Created document column mapping: {} -> {} for process {}", fieldName, columnName, processDefKey);

        return mapping;
    }

    /**
     * Get or create column mapping for a grid field.
     */
    @Transactional
    public ColumnMapping getOrCreateGridMapping(String processDefKey, String gridName, String fieldName, FieldType fieldType) {
        // 1. Check for existing mapping
        Optional<ColumnMapping> existing = columnMappingRepository.findGridFieldMapping(processDefKey, gridName, fieldName);
        if (existing.isPresent()) {
            return existing.get();
        }

        // 2. Calculate preferred column from hash
        int preferredColumn = calculatePreferredColumn(fieldName);

        // 3. Get used columns for this scope and type
        Set<Integer> usedColumns = columnMappingRepository.findUsedGridColumnIndices(processDefKey, gridName, fieldType);

        // 4. Find available column (nearest to preferred)
        int assignedColumn = findNearestAvailable(preferredColumn, usedColumns);

        // 5. Create and save mapping
        String columnName = (fieldType == FieldType.VARCHAR ? "varchar_" : "float_") + assignedColumn;

        ColumnMapping mapping = ColumnMapping.builder()
                .scopeType(ScopeType.GRID)
                .processDefinitionKey(processDefKey)
                .gridName(gridName)
                .fieldName(fieldName)
                .fieldType(fieldType)
                .columnName(columnName)
                .build();

        mapping = columnMappingRepository.save(mapping);
        log.info("Created grid column mapping: {}.{} -> {} for process {}", gridName, fieldName, columnName, processDefKey);

        return mapping;
    }

    /**
     * Get all document mappings for a process, keyed by field name.
     */
    @Transactional(readOnly = true)
    public Map<String, ColumnMapping> getDocumentMappings(String processDefKey) {
        List<ColumnMapping> mappings = columnMappingRepository.findAllDocumentMappings(processDefKey);
        Map<String, ColumnMapping> result = new HashMap<>();
        for (ColumnMapping mapping : mappings) {
            result.put(mapping.getFieldName(), mapping);
        }
        return result;
    }

    /**
     * Get all grid mappings for a specific grid, keyed by field name.
     */
    @Transactional(readOnly = true)
    public Map<String, ColumnMapping> getGridMappings(String processDefKey, String gridName) {
        List<ColumnMapping> mappings = columnMappingRepository.findAllGridMappings(processDefKey, gridName);
        Map<String, ColumnMapping> result = new HashMap<>();
        for (ColumnMapping mapping : mappings) {
            result.put(mapping.getFieldName(), mapping);
        }
        return result;
    }

    /**
     * Determine the field type based on the value.
     */
    public FieldType determineFieldType(Object value) {
        if (value == null) {
            return FieldType.VARCHAR; // Default to varchar for null
        }

        if (value instanceof Number) {
            return FieldType.FLOAT;
        }

        if (value instanceof Boolean) {
            return FieldType.FLOAT; // Store boolean as 0/1
        }

        // Try to parse as number
        if (value instanceof String strValue) {
            try {
                Double.parseDouble(strValue);
                // It's a valid number string, but we'll keep it as varchar
                // unless explicitly specified otherwise
            } catch (NumberFormatException ignored) {
                // Not a number
            }
        }

        return FieldType.VARCHAR;
    }

    /**
     * Calculate the preferred column index based on field name hash.
     * Uses consistent hashing so the same field name always gets the same preferred column.
     */
    private int calculatePreferredColumn(String fieldName) {
        int hash = Math.abs(fieldName.hashCode());
        return (hash % MAX_COLUMNS) + 1; // 1-30
    }

    /**
     * Find the nearest available column to the preferred one.
     * Searches outward: preferred, +1, -1, +2, -2, etc.
     */
    private int findNearestAvailable(int preferred, Set<Integer> used) {
        if (used == null) {
            used = new HashSet<>();
        }

        if (!used.contains(preferred)) {
            return preferred;
        }

        // Search outward from preferred
        for (int offset = 1; offset < MAX_COLUMNS; offset++) {
            int higher = preferred + offset;
            int lower = preferred - offset;

            // Wrap around for higher
            if (higher > MAX_COLUMNS) {
                higher = higher - MAX_COLUMNS;
            }

            // Wrap around for lower
            if (lower < 1) {
                lower = lower + MAX_COLUMNS;
            }

            if (!used.contains(higher)) {
                return higher;
            }
            if (!used.contains(lower)) {
                return lower;
            }
        }

        throw new RuntimeException("No available columns - all " + MAX_COLUMNS + " columns are used");
    }

    /**
     * Convert a value to the appropriate type for storage.
     */
    public Object convertValueForStorage(Object value, FieldType fieldType) {
        if (value == null) {
            return null;
        }

        if (fieldType == FieldType.FLOAT) {
            if (value instanceof Boolean boolValue) {
                return boolValue ? 1.0 : 0.0;
            }
            if (value instanceof Number numValue) {
                return numValue.doubleValue();
            }
            if (value instanceof String strValue) {
                try {
                    return Double.parseDouble(strValue);
                } catch (NumberFormatException e) {
                    log.warn("Could not convert string '{}' to double, returning null", strValue);
                    return null;
                }
            }
        }

        // VARCHAR - convert everything to string
        return value.toString();
    }

    /**
     * Convert a stored value back to its original type based on hints.
     */
    public Object convertValueFromStorage(Object storedValue, FieldType fieldType, String originalType) {
        if (storedValue == null) {
            return null;
        }

        if (fieldType == FieldType.FLOAT && storedValue instanceof Double doubleValue) {
            // Check if it was originally a boolean
            if ("boolean".equalsIgnoreCase(originalType) || "checkbox".equalsIgnoreCase(originalType)) {
                return doubleValue != 0;
            }
            // Check if it was an integer
            if ("integer".equalsIgnoreCase(originalType) || "int".equalsIgnoreCase(originalType)) {
                return doubleValue.intValue();
            }
            return doubleValue;
        }

        return storedValue;
    }
}
