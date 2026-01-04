package com.demo.bpm.service;

import com.demo.bpm.dto.DocumentDTO;
import com.demo.bpm.dto.GridRowDTO;
import com.demo.bpm.dto.ProcessConfigDTO;
import com.demo.bpm.entity.*;
import com.demo.bpm.entity.ColumnMapping.FieldType;
import com.demo.bpm.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
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

    public static final String DEFAULT_DOCUMENT_TYPE = "main";

    // ==================== Document Operations ====================

    /**
     * Save or update document data for a process instance (default type "main").
     */
    @Transactional
    public Document saveDocument(String processInstanceId, String businessKey,
                                  String processDefKey, String processDefName,
                                  Map<String, Object> variables, String userId) {
        return saveDocument(processInstanceId, businessKey, processDefKey, processDefName,
                DEFAULT_DOCUMENT_TYPE, variables, userId);
    }

    /**
     * Save or update document data for a process instance with specific type.
     */
    @Transactional
    public Document saveDocument(String processInstanceId, String businessKey,
                                  String processDefKey, String processDefName,
                                  String documentType, Map<String, Object> variables, String userId) {

        String docType = documentType != null ? documentType : DEFAULT_DOCUMENT_TYPE;

        // Get or create document by process instance ID and type
        Document document = documentRepository.findByProcessInstanceIdAndType(processInstanceId, docType)
                .orElseGet(() -> {
                    Document newDoc = new Document();
                    newDoc.setProcessInstanceId(processInstanceId);
                    newDoc.setType(docType);
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
                        processDefKey, docType, fieldName, fieldType);

                // Set value in appropriate column
                int columnIndex = mapping.getColumnIndex();
                Object convertedValue = columnMappingService.convertValueForStorage(value, fieldType);

                switch (fieldType) {
                    case VARCHAR -> document.setVarchar(columnIndex, (String) convertedValue);
                    case FLOAT -> document.setFloat(columnIndex, (Double) convertedValue);
                    case DATETIME -> document.setDatetime(columnIndex, (LocalDateTime) convertedValue);
                }
            }
        }

        document = documentRepository.save(document);
        log.info("Saved document type '{}' for process instance: {}", docType, processInstanceId);

        return document;
    }

    /**
     * Get all documents for a process instance.
     */
    @Transactional(readOnly = true)
    public Page<DocumentDTO> getDocumentsByProcessInstanceId(String processInstanceId, Pageable pageable) {
        return documentRepository.findByProcessInstanceId(processInstanceId, pageable)
                .map(this::convertToDTO);
    }

    /**
     * Get specific document by process instance ID and type.
     */
    @Transactional(readOnly = true)
    public Optional<DocumentDTO> getDocument(String processInstanceId, String documentType) {
        String docType = documentType != null ? documentType : DEFAULT_DOCUMENT_TYPE;
        return documentRepository.findByProcessInstanceIdAndType(processInstanceId, docType)
                .map(this::convertToDTO);
    }

    /**
     * Get document by process instance ID (default type "main").
     * @deprecated Use getDocument(processInstanceId, type) instead
     */
    @Deprecated
    @Transactional(readOnly = true)
    public Optional<DocumentDTO> getDocumentByProcessInstanceId(String processInstanceId) {
        return getDocument(processInstanceId, DEFAULT_DOCUMENT_TYPE);
    }

    /**
     * Get document by business key and type.
     */
    @Transactional(readOnly = true)
    public Optional<DocumentDTO> getDocumentByBusinessKey(String businessKey, String documentType) {
        String docType = documentType != null ? documentType : DEFAULT_DOCUMENT_TYPE;
        return documentRepository.findByBusinessKeyAndType(businessKey, docType)
                .map(this::convertToDTO);
    }

    /**
     * Get all documents by business key.
     */
    @Transactional(readOnly = true)
    public Page<DocumentDTO> getDocumentsByBusinessKey(String businessKey, Pageable pageable) {
        return documentRepository.findByBusinessKey(businessKey, pageable)
                .map(this::convertToDTO);
    }

    // ==================== Grid Row Operations ====================

    /**
     * Save grid rows for a document (default type "main").
     * Replaces existing rows for the specified grid.
     */
    @Transactional
    public List<GridRow> saveGridRows(String processInstanceId, String processDefKey,
                                       String gridName, List<Map<String, Object>> rows) {
        return saveGridRows(processInstanceId, processDefKey, DEFAULT_DOCUMENT_TYPE, gridName, rows);
    }

    /**
     * Save grid rows for a document with specific type.
     * Replaces existing rows for the specified grid.
     */
    @Transactional
    public List<GridRow> saveGridRows(String processInstanceId, String processDefKey,
                                       String documentType, String gridName,
                                       List<Map<String, Object>> rows) {

        String docType = documentType != null ? documentType : DEFAULT_DOCUMENT_TYPE;

        // Get document by type
        Document document = documentRepository.findByProcessInstanceIdAndType(processInstanceId, docType)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Document type '" + docType + "' not found for process instance: " + processInstanceId));

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
                        processDefKey, docType, gridName, fieldName, fieldType);

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

        log.info("Saved {} rows for grid '{}' in document type '{}' for process {}",
                savedRows.size(), gridName, docType, processInstanceId);
        return savedRows;
    }

    /**
     * Get grid rows for a document (default type "main").
     */
    @Transactional(readOnly = true)
    public Page<GridRowDTO> getGridRows(String processInstanceId, String gridName, Pageable pageable) {
        return getGridRows(processInstanceId, DEFAULT_DOCUMENT_TYPE, gridName, pageable);
    }

    /**
     * Get grid rows for a document with specific type.
     */
    @Transactional(readOnly = true)
    public Page<GridRowDTO> getGridRows(String processInstanceId, String documentType, String gridName, Pageable pageable) {
        String docType = documentType != null ? documentType : DEFAULT_DOCUMENT_TYPE;

        Document document = documentRepository.findByProcessInstanceIdAndType(processInstanceId, docType)
                .orElse(null);
        if (document == null) {
            return Page.empty(pageable);
        }

        Page<GridRow> rows = gridRowRepository.findByDocumentIdAndGridNameOrderByRowIndex(
                document.getId(), gridName, pageable);

        String processDefKey = document.getProcessDefinitionKey();
        Map<String, ColumnMapping> mappings = columnMappingService.getGridMappings(processDefKey, docType, gridName);

        return rows.map(row -> convertGridRowToDTO(row, mappings));
    }

    /**
     * Delete grid rows for a document type.
     */
    @Transactional
    public void deleteGridRows(String processInstanceId, String documentType, String gridName) {
        String docType = documentType != null ? documentType : DEFAULT_DOCUMENT_TYPE;

        Document document = documentRepository.findByProcessInstanceIdAndType(processInstanceId, docType)
                .orElse(null);
        if (document != null) {
            gridRowRepository.deleteByDocumentIdAndGridName(document.getId(), gridName);
            log.info("Deleted grid '{}' rows from document type '{}' for process {}",
                    gridName, docType, processInstanceId);
        }
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
     * Save all data (document + grids) for a process instance with default type.
     * Uses REQUIRES_NEW propagation to run in a separate transaction,
     * preventing rollback-only issues when called from other transactional methods.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveAllData(String processInstanceId, String businessKey,
                            String processDefKey, String processDefName,
                            Map<String, Object> variables, String userId) {
        saveAllData(processInstanceId, businessKey, processDefKey, processDefName, DEFAULT_DOCUMENT_TYPE, variables, userId);
    }

    /**
     * Save all data (document + grids) for a process instance with specific type.
     * Uses REQUIRES_NEW propagation to run in a separate transaction,
     * preventing rollback-only issues when called from other transactional methods.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveAllData(String processInstanceId, String businessKey,
                            String processDefKey, String processDefName,
                            String documentType, Map<String, Object> variables, String userId) {

        String docType = documentType != null ? documentType : DEFAULT_DOCUMENT_TYPE;

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
        saveDocument(processInstanceId, businessKey, processDefKey, processDefName, docType, documentVars, userId);

        // Save grid data
        for (Map.Entry<String, List<Map<String, Object>>> entry : grids.entrySet()) {
            saveGridRows(processInstanceId, processDefKey, docType, entry.getKey(), entry.getValue());
        }
    }

    // ==================== Conversion Methods ====================

    private DocumentDTO convertToDTO(Document document) {
        String processDefKey = document.getProcessDefinitionKey();
        String docType = document.getType() != null ? document.getType() : DEFAULT_DOCUMENT_TYPE;
        Map<String, ColumnMapping> mappings = columnMappingService.getDocumentMappings(processDefKey, docType);

        // Convert column values back to field names
        Map<String, Object> fields = new HashMap<>();
        for (ColumnMapping mapping : mappings.values()) {
            int columnIndex = mapping.getColumnIndex();
            Object value;

            switch (mapping.getFieldType()) {
                case VARCHAR -> value = document.getVarchar(columnIndex);
                case FLOAT -> value = document.getFloat(columnIndex);
                case DATETIME -> {
                    LocalDateTime dt = document.getDatetime(columnIndex);
                    // Convert to ISO string for JSON serialization
                    value = dt != null ? dt.toString() : null;
                }
                default -> value = null;
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
            Map<String, ColumnMapping> gridMappings = columnMappingService.getGridMappings(processDefKey, docType, gridName);

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
                .type(docType)
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
