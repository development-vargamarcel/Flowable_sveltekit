package com.demo.bpm.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Entity
@Table(name = "column_mapping",
       uniqueConstraints = @UniqueConstraint(
           name = "uk_column_mapping",
           columnNames = {"scope_type", "process_definition_key", "grid_name", "field_name"}
       ))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ColumnMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scope_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ScopeType scopeType;

    @Column(name = "process_definition_key", nullable = false, length = 255)
    private String processDefinitionKey;

    @Column(name = "grid_name", length = 255)
    private String gridName;

    @Column(name = "field_name", nullable = false, length = 255)
    private String fieldName;

    @Column(name = "field_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private FieldType fieldType;

    @Column(name = "column_name", nullable = false, length = 20)
    private String columnName;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum ScopeType {
        DOCUMENT,
        GRID
    }

    public enum FieldType {
        VARCHAR,
        FLOAT,
        DATETIME
    }

    /**
     * Extract the column index from the column name.
     * e.g., "varchar_7" -> 7, "float_12" -> 12
     */
    public int getColumnIndex() {
        if (columnName == null || !columnName.contains("_")) {
            throw new IllegalStateException("Invalid column name: " + columnName);
        }
        String indexStr = columnName.substring(columnName.lastIndexOf('_') + 1);
        return Integer.parseInt(indexStr);
    }
}
