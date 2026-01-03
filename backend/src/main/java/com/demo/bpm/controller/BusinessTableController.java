package com.demo.bpm.controller;

import com.demo.bpm.dto.DocumentDTO;
import com.demo.bpm.dto.GridRowDTO;
import com.demo.bpm.dto.ProcessConfigDTO;
import com.demo.bpm.dto.ProcessInstanceDTO;
import com.demo.bpm.entity.ProcessConfig;
import com.demo.bpm.service.BusinessTableService;
import com.demo.bpm.service.ProcessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for business table operations.
 * Provides endpoints for managing documents and grid data.
 */
@RestController
@RequestMapping("/api/business")
@RequiredArgsConstructor
@Slf4j
public class BusinessTableController {

    private final BusinessTableService businessTableService;
    private final ProcessService processService;

    // ==================== Document Endpoints ====================

    /**
     * Get all documents for a process instance.
     */
    @GetMapping("/processes/{processInstanceId}/documents")
    public ResponseEntity<Page<DocumentDTO>> getAllDocuments(
            @PathVariable String processInstanceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<DocumentDTO> documents = businessTableService.getDocumentsByProcessInstanceId(processInstanceId, PageRequest.of(page, size));
        return ResponseEntity.ok(documents);
    }

    /**
     * Get specific document by process instance ID and type.
     */
    @GetMapping("/processes/{processInstanceId}/documents/{type}")
    public ResponseEntity<DocumentDTO> getDocument(
            @PathVariable String processInstanceId,
            @PathVariable String type) {

        return businessTableService.getDocument(processInstanceId, type)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Save document data with type.
     */
    @PostMapping("/processes/{processInstanceId}/documents/{type}")
    public ResponseEntity<DocumentDTO> saveDocumentWithType(
            @PathVariable String processInstanceId,
            @PathVariable String type,
            @RequestBody SaveDocumentRequest request) {

        businessTableService.saveDocument(
                processInstanceId,
                request.getBusinessKey(),
                request.getProcessDefinitionKey(),
                request.getProcessDefinitionName(),
                type,
                request.getVariables(),
                request.getUserId()
        );

        return businessTableService.getDocument(processInstanceId, type)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.internalServerError().build());
    }

    /**
     * Get document by process instance ID (legacy - returns main type).
     */
    @GetMapping("/documents/{processInstanceId}")
    public ResponseEntity<DocumentDTO> getDocumentByProcessInstanceId(
            @PathVariable String processInstanceId) {

        return businessTableService.getDocument(processInstanceId, "main")
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get document by business key (legacy - returns main type).
     */
    @GetMapping("/documents/by-business-key/{businessKey}")
    public ResponseEntity<DocumentDTO> getDocumentByBusinessKey(
            @PathVariable String businessKey) {

        return businessTableService.getDocumentByBusinessKey(businessKey, "main")
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all documents by business key.
     */
    @GetMapping("/documents/all/by-business-key/{businessKey}")
    public ResponseEntity<Page<DocumentDTO>> getAllDocumentsByBusinessKey(
            @PathVariable String businessKey,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<DocumentDTO> documents = businessTableService.getDocumentsByBusinessKey(businessKey, PageRequest.of(page, size));
        return ResponseEntity.ok(documents);
    }

    /**
     * Save document data (legacy - saves as main type).
     */
    @PostMapping("/documents")
    public ResponseEntity<DocumentDTO> saveDocument(@RequestBody SaveDocumentRequest request) {
        businessTableService.saveDocument(
                request.getProcessInstanceId(),
                request.getBusinessKey(),
                request.getProcessDefinitionKey(),
                request.getProcessDefinitionName(),
                request.getDocumentType() != null ? request.getDocumentType() : "main",
                request.getVariables(),
                request.getUserId()
        );

        return businessTableService.getDocument(
                request.getProcessInstanceId(),
                request.getDocumentType() != null ? request.getDocumentType() : "main"
        )
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.internalServerError().build());
    }

    /**
     * Save draft - creates a process instance if needed and saves all data (document + grids).
     */
    @PostMapping("/save-draft")
    public ResponseEntity<?> saveDraft(@RequestBody SaveDraftRequest request) {
        try {
            String processInstanceId = request.getProcessInstanceId();

            // If no process instance exists, create a draft process instance
            if (processInstanceId == null || processInstanceId.trim().isEmpty()) {
                String businessKey = request.getBusinessKey();
                if (businessKey == null || businessKey.trim().isEmpty()) {
                    businessKey = request.getProcessDefinitionKey().toUpperCase() + "-DRAFT-" + System.currentTimeMillis();
                }

                ProcessInstanceDTO processInstance = processService.startProcess(
                        request.getProcessDefinitionKey(),
                        businessKey,
                        request.getVariables(),
                        request.getUserId()
                );

                processInstanceId = processInstance.getId();
            }

            // Save all data (document + grids)
            businessTableService.saveAllData(
                    processInstanceId,
                    request.getBusinessKey(),
                    request.getProcessDefinitionKey(),
                    request.getProcessDefinitionName(),
                    request.getVariables(),
                    request.getUserId()
            );

            log.info("Saved draft for process instance: {}", processInstanceId);

            return ResponseEntity.ok(Map.of(
                    "message", "Draft saved successfully",
                    "processInstanceId", processInstanceId
            ));
        } catch (Exception e) {
            log.error("Error saving draft: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    // ==================== Grid Row Endpoints ====================

    /**
     * Get grid rows for a process instance, document type, and grid name.
     */
    @GetMapping("/processes/{processInstanceId}/documents/{type}/grids/{gridName}")
    public ResponseEntity<Page<GridRowDTO>> getGridRowsWithType(
            @PathVariable String processInstanceId,
            @PathVariable String type,
            @PathVariable String gridName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<GridRowDTO> rows = businessTableService.getGridRows(processInstanceId, type, gridName, PageRequest.of(page, size));
        return ResponseEntity.ok(rows);
    }

    /**
     * Save grid rows with document type.
     */
    @PostMapping("/processes/{processInstanceId}/documents/{type}/grids/{gridName}")
    public ResponseEntity<Page<GridRowDTO>> saveGridRowsWithType(
            @PathVariable String processInstanceId,
            @PathVariable String type,
            @PathVariable String gridName,
            @RequestBody SaveGridRowsRequest request) {

        businessTableService.saveGridRows(
                processInstanceId,
                request.getProcessDefinitionKey(),
                type,
                gridName,
                request.getRows()
        );

        Page<GridRowDTO> rows = businessTableService.getGridRows(processInstanceId, type, gridName, PageRequest.of(0, 10));
        return ResponseEntity.ok(rows);
    }

    /**
     * Delete grid rows with document type.
     */
    @DeleteMapping("/processes/{processInstanceId}/documents/{type}/grids/{gridName}")
    public ResponseEntity<Void> deleteGridRowsWithType(
            @PathVariable String processInstanceId,
            @PathVariable String type,
            @PathVariable String gridName) {

        businessTableService.deleteGridRows(processInstanceId, type, gridName);
        return ResponseEntity.ok().build();
    }

    /**
     * Get grid rows for a process instance and grid name (legacy - uses main type).
     */
    @GetMapping("/documents/{processInstanceId}/grids/{gridName}")
    public ResponseEntity<Page<GridRowDTO>> getGridRows(
            @PathVariable String processInstanceId,
            @PathVariable String gridName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<GridRowDTO> rows = businessTableService.getGridRows(processInstanceId, "main", gridName, PageRequest.of(page, size));
        return ResponseEntity.ok(rows);
    }

    /**
     * Save grid rows (legacy - uses main type).
     */
    @PostMapping("/documents/{processInstanceId}/grids/{gridName}")
    public ResponseEntity<Page<GridRowDTO>> saveGridRows(
            @PathVariable String processInstanceId,
            @PathVariable String gridName,
            @RequestBody SaveGridRowsRequest request) {

        String docType = request.getDocumentType() != null ? request.getDocumentType() : "main";
        businessTableService.saveGridRows(
                processInstanceId,
                request.getProcessDefinitionKey(),
                docType,
                gridName,
                request.getRows()
        );

        Page<GridRowDTO> rows = businessTableService.getGridRows(processInstanceId, docType, gridName, PageRequest.of(0, 10));
        return ResponseEntity.ok(rows);
    }

    /**
     * Delete grid rows for a specific grid (legacy - uses main type).
     */
    @DeleteMapping("/documents/{processInstanceId}/grids/{gridName}")
    public ResponseEntity<Void> deleteGridRows(
            @PathVariable String processInstanceId,
            @PathVariable String gridName,
            @RequestParam(required = false) String documentType) {

        String docType = documentType != null ? documentType : "main";
        businessTableService.deleteGridRows(processInstanceId, docType, gridName);
        return ResponseEntity.ok().build();
    }

    // ==================== Process Config Endpoints ====================

    /**
     * Get process configuration.
     */
    @GetMapping("/config/{processDefinitionKey}")
    public ResponseEntity<ProcessConfigDTO> getProcessConfig(
            @PathVariable String processDefinitionKey) {

        ProcessConfig config = businessTableService.getOrCreateProcessConfig(processDefinitionKey);
        return ResponseEntity.ok(convertToDTO(config));
    }

    /**
     * Update process configuration.
     */
    @PutMapping("/config/{processDefinitionKey}")
    public ResponseEntity<ProcessConfigDTO> updateProcessConfig(
            @PathVariable String processDefinitionKey,
            @RequestBody UpdateProcessConfigRequest request) {

        ProcessConfig config = businessTableService.updateProcessConfig(
                processDefinitionKey,
                request.getPersistOnTaskComplete(),
                request.getPersistOnProcessComplete()
        );
        return ResponseEntity.ok(convertToDTO(config));
    }

    // ==================== Helper Methods ====================

    private ProcessConfigDTO convertToDTO(ProcessConfig config) {
        return ProcessConfigDTO.builder()
                .id(config.getId())
                .processDefinitionKey(config.getProcessDefinitionKey())
                .persistOnTaskComplete(config.getPersistOnTaskComplete())
                .persistOnProcessComplete(config.getPersistOnProcessComplete())
                .build();
    }

    // ==================== Request DTOs ====================

    @lombok.Data
    public static class SaveDocumentRequest {
        private String processInstanceId;
        private String businessKey;
        private String processDefinitionKey;
        private String processDefinitionName;
        private String documentType;
        private Map<String, Object> variables;
        private String userId;
    }

    @lombok.Data
    public static class SaveGridRowsRequest {
        private String processDefinitionKey;
        private String documentType;
        private List<Map<String, Object>> rows;
    }

    @lombok.Data
    public static class UpdateProcessConfigRequest {
        private Boolean persistOnTaskComplete;
        private Boolean persistOnProcessComplete;
    }

    @lombok.Data
    public static class SaveDraftRequest {
        private String processInstanceId;
        private String businessKey;
        private String processDefinitionKey;
        private String processDefinitionName;
        private String documentType;
        private Map<String, Object> variables;
        private String userId;
    }
}
