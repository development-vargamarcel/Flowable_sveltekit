package com.demo.bpm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.*;

/**
 * Service for database table browsing operations.
 * Allows listing tables and querying data with pagination.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DatabaseService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Get list of all tables in the database.
     */
    public List<String> listTables() {
        List<String> tables = new ArrayList<>();

        try {
            jdbcTemplate.execute((java.sql.Connection connection) -> {
                DatabaseMetaData metaData = connection.getMetaData();
                try (ResultSet rs = metaData.getTables(null, null, "%", new String[]{"TABLE"})) {
                    while (rs.next()) {
                        String tableName = rs.getString("TABLE_NAME");
                        // Exclude system tables
                        if (!tableName.startsWith("SYSTEM_")) {
                            tables.add(tableName);
                        }
                    }
                }
                return null;
            });
        } catch (Exception e) {
            log.error("Error listing tables", e);
        }

        Collections.sort(tables);
        return tables;
    }

    /**
     * Get column metadata for a table.
     */
    public List<Map<String, Object>> getTableColumns(String tableName) {
        List<Map<String, Object>> columns = new ArrayList<>();

        // Validate table name to prevent SQL injection
        if (!isValidTableName(tableName)) {
            throw new IllegalArgumentException("Invalid table name: " + tableName);
        }

        try {
            jdbcTemplate.execute((java.sql.Connection connection) -> {
                DatabaseMetaData metaData = connection.getMetaData();
                try (ResultSet rs = metaData.getColumns(null, null, tableName.toUpperCase(), null)) {
                    while (rs.next()) {
                        Map<String, Object> column = new LinkedHashMap<>();
                        column.put("name", rs.getString("COLUMN_NAME"));
                        column.put("type", rs.getString("TYPE_NAME"));
                        column.put("size", rs.getInt("COLUMN_SIZE"));
                        column.put("nullable", rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable);
                        columns.add(column);
                    }
                }
                return null;
            });
        } catch (Exception e) {
            log.error("Error getting columns for table: " + tableName, e);
        }

        return columns;
    }

    /**
     * Get total row count for a table.
     */
    public long getTableRowCount(String tableName) {
        // Validate table name to prevent SQL injection
        if (!isValidTableName(tableName)) {
            throw new IllegalArgumentException("Invalid table name: " + tableName);
        }

        String sql = "SELECT COUNT(*) FROM " + sanitizeTableName(tableName);
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count != null ? count : 0;
    }

    /**
     * Get rows from a table with pagination.
     */
    public TableData getTableData(String tableName, int page, int size) {
        // Validate table name to prevent SQL injection
        if (!isValidTableName(tableName)) {
            throw new IllegalArgumentException("Invalid table name: " + tableName);
        }

        // Ensure reasonable limits
        if (page < 0) page = 0;
        if (size < 1) size = 10;
        if (size > 1000) size = 1000;

        String safeTableName = sanitizeTableName(tableName);
        long totalRows = getTableRowCount(tableName);
        int totalPages = (int) Math.ceil((double) totalRows / size);

        List<String> columnNames = new ArrayList<>();
        List<Map<String, Object>> rows = new ArrayList<>();

        int offset = page * size;
        String sql = "SELECT * FROM " + safeTableName + " LIMIT " + size + " OFFSET " + offset;

        try {
            jdbcTemplate.query(sql, (ResultSet rs) -> {
                // Get column names on first row
                if (columnNames.isEmpty()) {
                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();
                    for (int i = 1; i <= columnCount; i++) {
                        columnNames.add(metaData.getColumnName(i));
                    }
                }

                // Build row data
                Map<String, Object> row = new LinkedHashMap<>();
                for (String colName : columnNames) {
                    Object value = rs.getObject(colName);
                    // Handle special types
                    if (value instanceof java.sql.Clob) {
                        try {
                            java.sql.Clob clob = (java.sql.Clob) value;
                            value = clob.getSubString(1, (int) Math.min(clob.length(), 1000));
                            if (clob.length() > 1000) {
                                value = value + "... (truncated)";
                            }
                        } catch (Exception e) {
                            value = "[CLOB]";
                        }
                    } else if (value instanceof byte[]) {
                        value = "[BLOB: " + ((byte[]) value).length + " bytes]";
                    }
                    row.put(colName, value);
                }
                rows.add(row);
            });
        } catch (Exception e) {
            log.error("Error querying table: " + tableName, e);
            throw new RuntimeException("Error querying table: " + e.getMessage());
        }

        return new TableData(tableName, columnNames, rows, page, size, totalRows, totalPages);
    }

    /**
     * Validate table name to prevent SQL injection.
     */
    private boolean isValidTableName(String tableName) {
        if (tableName == null || tableName.isEmpty()) {
            return false;
        }
        // Only allow alphanumeric characters and underscores
        return tableName.matches("^[a-zA-Z_][a-zA-Z0-9_]*$");
    }

    /**
     * Sanitize table name (uppercase for H2).
     */
    private String sanitizeTableName(String tableName) {
        return tableName.toUpperCase();
    }

    /**
     * Data class for paginated table results.
     */
    public record TableData(
            String tableName,
            List<String> columns,
            List<Map<String, Object>> rows,
            int page,
            int size,
            long totalRows,
            int totalPages
    ) {}
}
