package com.demo.bpm.service;

import com.demo.bpm.dto.DocumentDTO;
import com.demo.bpm.dto.GridRowDTO;
import com.demo.bpm.dto.ProcessConfigDTO;
import com.demo.bpm.entity.*;
import com.demo.bpm.entity.ColumnMapping.FieldType;
import com.demo.bpm.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing business tables (document and grid_rows).
 * Handles saving and loading process variables to/from structured tables.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BusinessTableService {

    private final DocumentRepository documentRepository;
    private final GridRowRepository gridRowRepository;
    private final ProcessConfigRepository processConfigRepository;
    private final ColumnMappingService columnMappingService;

    // ==================== Document Operations ====================

    /**
     * Save or update document data for a process instance.
     */
    @Transactional
    public Document saveDocument(String processInstanceId, String businessKey,
                                  String processDefKey, String processDefName,
                                  Map<String, Object> variables, String userId) {

        // Get or create document
        Document document = documentRepository.findByProcessInstanceId(processInstanceId)
                .orElseGet(() -> {
                    Document newDoc = new Document();
                    newDoc.setProcessInstanceId(processInstanceId);
                    newDoc.setCreatedBy(userId);
                    return newDoc;
                });

        document.setBusinessKey(businessKey);
        document.setProcessDefinitionKey(processDefKey);
        document.setProcessDefinitionName(processDefName);
        document.setUpdatedBy(userId);

        // Map and set field values
        if (variables != null) {
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                String fieldName = entry.getKey();
                Object value = entry.getValue();

                // Skip grid data (handled separately) and null values
                if (value instanceof List || fieldName.startsWith("_")) {
                    continue;
                }

                // Determine field type and get/create mapping
                FieldType fieldType = columnMappingService.determineFieldType(value);
                ColumnMapping mapping = columnMappingService.getOrCreateDocumentMapping(
                        processDefKey, fieldName, fieldType);

                // Set value in appropriate column
                int columnIndex = mapping.getColumnIndex();
                Object convertedValue = columnMappingService.convertValueForStorage(value, fieldType);

                if (fieldType == FieldType.VARCHAR) {
                    document.setVarchar(columnIndex, (String) convertedValue);
                } else {
                    document.setFloat(columnIndex, (Double) convertedValue);
                }
            }
        }

        document = documentRepository.save(document);
        log.info("Saved document for process instance: {}", processInstanceId);

        return document;
    }

    /**
     * Get document by process instance ID.
     */
    @Transactional(readOnly = true)
    public Optional<DocumentDTO> getDocumentByProcessInstanceId(String processInstanceId) {
        return documentRepository.findByProcessInstanceId(processInstanceId)
                .map(this::convertToDTO);
    }

    /**
     * Get document by business key.
     */
    @Transactional(readOnly = true)
    public Optional<DocumentDTO> getDocumentByBusinessKey(String businessKey) {
        return documentRepository.findByBusinessKey(businessKey)
                .map(this::convertToDTO);
    }

    // ==================== Grid Row Operations ====================

    /**
     * Save grid rows for a document.
     * Replaces existing rows for the specified grid.
     */
    @Transactional
    public List<GridRow> saveGridRows(String processInstanceId, String processDefKey,
                                       String gridName, List<Map<String, Object>> rows) {

        // Get document
        Document document = documentRepository.findByProcessInstanceId(processInstanceId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Document not found for process instance: " + processInstanceId));

        // Delete existing rows for this grid
        gridRowRepository.deleteByDocumentIdAndGridName(document.getId(), gridName);

        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }

        List<GridRow> savedRows = new ArrayList<>();

        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> rowData = rows.get(i);

            GridRow gridRow = new GridRow();
            gridRow.setDocument(document);
            gridRow.setProcessInstanceId(processInstanceId);
            gridRow.setGridName(gridName);
            gridRow.setRowIndex(i);

            // Map each field in the row
            for (Map.Entry<String, Object> entry : rowData.entrySet()) {
                String fieldName = entry.getKey();
                Object value = entry.getValue();

                if (value == null || fieldName.startsWith("_")) {
                    continue;
                }

                // Determine field type and get/create mapping
                FieldType fieldType = columnMappingService.determineFieldType(value);
                ColumnMapping mapping = columnMappingService.getOrCreateGridMapping(
                        processDefKey, gridName, fieldName, fieldType);

                // Set value in appropriate column
                int columnIndex = mapping.getColumnIndex();
                Object convertedValue = columnMappingService.convertValueForStorage(value, fieldType);

                switch (fieldType) {
                    case VARCHAR -> gridRow.setVarchar(columnIndex, (String) convertedValue);
                    case FLOAT -> gridRow.setFloat(columnIndex, (Double) convertedValue);
                    case DATETIME -> gridRow.setDatetime(columnIndex, (LocalDateTime) convertedValue);
                }
            }

            savedRows.add(gridRowRepository.save(gridRow));
        }

        log.info("Saved {} rows for grid '{}' in process {}", savedRows.size(), gridName, processInstanceId);
        return savedRows;
    }

    /**
     * Get grid rows for a document.
     */
    @Transactional(readOnly = true)
    public List<GridRowDTO> getGridRows(String processInstanceId, String gridName) {
        Document document = documentRepository.findByProcessInstanceId(processInstanceId).orElse(null);
        if (document == null) {
            return Collections.emptyList();
        }

        List<GridRow> rows = gridRowRepository.findByDocumentIdAndGridNameOrderByRowIndex(
                document.getId(), gridName);

        String processDefKey = document.getProcessDefinitionKey();
        Map<String, ColumnMapping> mappings = columnMappingService.getGridMappings(processDefKey, gridName);

        return rows.stream()
                .map(row -> convertGridRowToDTO(row, mappings))
                .collect(Collectors.toList());
    }

    // ==================== Process Config Operations ====================

    /**
     * Get or create process configuration.
     */
    @Transactional
    public ProcessConfig getOrCreateProcessConfig(String processDefKey) {
        return processConfigRepository.findByProcessDefinitionKey(processDefKey)
                .orElseGet(() -> {
                    ProcessConfig config = ProcessConfig.builder()
                            .processDefinitionKey(processDefKey)
                            .persistOnTaskComplete(true)
                            .persistOnProcessComplete(true)
                            .build();
                    return processConfigRepository.save(config);
                });
    }

    /**
     * Update process configuration.
     */
    @Transactional
    public ProcessConfig updateProcessConfig(String processDefKey, Boolean persistOnTask, Boolean persistOnProcess) {
        ProcessConfig config = getOrCreateProcessConfig(processDefKey);

        if (persistOnTask != null) {
            config.setPersistOnTaskComplete(persistOnTask);
        }
        if (persistOnProcess != null) {
            config.setPersistOnProcessComplete(persistOnProcess);
        }

        return processConfigRepository.save(config);
    }

    /**
     * Check if should persist on task complete.
     */
    @Transactional(readOnly = true)
    public boolean shouldPersistOnTaskComplete(String processDefKey) {
        return processConfigRepository.findByProcessDefinitionKey(processDefKey)
                .map(ProcessConfig::getPersistOnTaskComplete)
                .orElse(true); // Default to true
    }

    /**
     * Check if should persist on process complete.
     */
    @Transactional(readOnly = true)
    public boolean shouldPersistOnProcessComplete(String processDefKey) {
        return processConfigRepository.findByProcessDefinitionKey(processDefKey)
                .map(ProcessConfig::getPersistOnProcessComplete)
                .orElse(true); // Default to true
    }

    // ==================== Combined Operations ====================

    /**
     * Save all data (document + grids) for a process instance.
     */
    @Transactional
    public void saveAllData(String processInstanceId, String businessKey,
                            String processDefKey, String processDefName,
                            Map<String, Object> variables, String userId) {

        // Extract grid data from variables
        Map<String, List<Map<String, Object>>> grids = new HashMap<>();
        Map<String, Object> documentVars = new HashMap<>();

        if (variables != null) {
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();

                if (value instanceof List<?> listValue && !listValue.isEmpty()) {
                    Object firstItem = listValue.get(0);
                    if (firstItem instanceof Map) {
                        // This is grid data
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> gridData = (List<Map<String, Object>>) listValue;
                        grids.put(key, gridData);
                        continue;
                    }
                }

                documentVars.put(key, value);
            }
        }

        // Save document first
        saveDocument(processInstanceId, businessKey, processDefKey, processDefName, documentVars, userId);

        // Save grid data
        for (Map.Entry<String, List<Map<String, Object>>> entry : grids.entrySet()) {
            saveGridRows(processInstanceId, processDefKey, entry.getKey(), entry.getValue());
        }
    }

    // ==================== Conversion Methods ====================

    private DocumentDTO convertToDTO(Document document) {
        String processDefKey = document.getProcessDefinitionKey();
        Map<String, ColumnMapping> mappings = columnMappingService.getDocumentMappings(processDefKey);

        // Convert column values back to field names
        Map<String, Object> fields = new HashMap<>();
        for (ColumnMapping mapping : mappings.values()) {
            int columnIndex = mapping.getColumnIndex();
            Object value;

            if (mapping.getFieldType() == FieldType.VARCHAR) {
                value = document.getVarchar(columnIndex);
            } else {
                value = document.getFloat(columnIndex);
            }

            if (value != null) {
                fields.put(mapping.getFieldName(), value);
            }
        }

        // Get grid data
        Map<String, List<Map<String, Object>>> grids = new HashMap<>();
        List<GridRow> allRows = gridRowRepository.findByDocumentIdOrderByGridNameAscRowIndexAsc(document.getId());

        // Group by grid name
        Map<String, List<GridRow>> groupedRows = allRows.stream()
                .collect(Collectors.groupingBy(GridRow::getGridName));

        for (Map.Entry<String, List<GridRow>> entry : groupedRows.entrySet()) {
            String gridName = entry.getKey();
            Map<String, ColumnMapping> gridMappings = columnMappingService.getGridMappings(processDefKey, gridName);

            List<Map<String, Object>> rowData = entry.getValue().stream()
                    .map(row -> convertGridRowToMap(row, gridMappings))
                    .collect(Collectors.toList());

            grids.put(gridName, rowData);
        }

        return DocumentDTO.builder()
                .id(document.getId())
                .processInstanceId(document.getProcessInstanceId())
                .businessKey(document.getBusinessKey())
                .processDefinitionKey(document.getProcessDefinitionKey())
                .processDefinitionName(document.getProcessDefinitionName())
                .fields(fields)
                .grids(grids)
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .createdBy(document.getCreatedBy())
                .updatedBy(document.getUpdatedBy())
                .build();
    }

    private GridRowDTO convertGridRowToDTO(GridRow row, Map<String, ColumnMapping> mappings) {
        Map<String, Object> fields = convertGridRowToMap(row, mappings);

        return GridRowDTO.builder()
                .id(row.getId())
                .documentId(row.getDocument().getId())
                .processInstanceId(row.getProcessInstanceId())
                .gridName(row.getGridName())
                .rowIndex(row.getRowIndex())
                .fields(fields)
                .build();
    }

    private Map<String, Object> convertGridRowToMap(GridRow row, Map<String, ColumnMapping> mappings) {
        Map<String, Object> fields = new HashMap<>();

        for (ColumnMapping mapping : mappings.values()) {
            int columnIndex = mapping.getColumnIndex();
            Object value;

            switch (mapping.getFieldType()) {
                case VARCHAR -> value = row.getVarchar(columnIndex);
                case FLOAT -> value = row.getFloat(columnIndex);
                case DATETIME -> {
                    LocalDateTime dt = row.getDatetime(columnIndex);
                    // Convert to ISO string for JSON serialization
                    value = dt != null ? dt.toString() : null;
                }
                default -> value = null;
            }

            if (value != null) {
                fields.put(mapping.getFieldName(), value);
            }
        }

        return fields;
    }
}
