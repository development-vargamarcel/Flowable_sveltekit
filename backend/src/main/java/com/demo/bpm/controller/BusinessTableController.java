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
     * Get document by process instance ID.
     */
    @GetMapping("/documents/{processInstanceId}")
    public ResponseEntity<DocumentDTO> getDocumentByProcessInstanceId(
            @PathVariable String processInstanceId) {

        return businessTableService.getDocumentByProcessInstanceId(processInstanceId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get document by business key.
     */
    @GetMapping("/documents/by-business-key/{businessKey}")
    public ResponseEntity<DocumentDTO> getDocumentByBusinessKey(
            @PathVariable String businessKey) {

        return businessTableService.getDocumentByBusinessKey(businessKey)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Save document data.
     */
    @PostMapping("/documents")
    public ResponseEntity<DocumentDTO> saveDocument(@RequestBody SaveDocumentRequest request) {
        businessTableService.saveDocument(
                request.getProcessInstanceId(),
                request.getBusinessKey(),
                request.getProcessDefinitionKey(),
                request.getProcessDefinitionName(),
                request.getVariables(),
                request.getUserId()
        );

        return businessTableService.getDocumentByProcessInstanceId(request.getProcessInstanceId())
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
     * Get grid rows for a process instance and grid name.
     */
    @GetMapping("/documents/{processInstanceId}/grids/{gridName}")
    public ResponseEntity<List<GridRowDTO>> getGridRows(
            @PathVariable String processInstanceId,
            @PathVariable String gridName) {

        List<GridRowDTO> rows = businessTableService.getGridRows(processInstanceId, gridName);
        return ResponseEntity.ok(rows);
    }

    /**
     * Save grid rows.
     */
    @PostMapping("/documents/{processInstanceId}/grids/{gridName}")
    public ResponseEntity<List<GridRowDTO>> saveGridRows(
            @PathVariable String processInstanceId,
            @PathVariable String gridName,
            @RequestBody SaveGridRowsRequest request) {

        businessTableService.saveGridRows(
                processInstanceId,
                request.getProcessDefinitionKey(),
                gridName,
                request.getRows()
        );

        List<GridRowDTO> rows = businessTableService.getGridRows(processInstanceId, gridName);
        return ResponseEntity.ok(rows);
    }

    /**
     * Delete grid rows for a specific grid.
     */
    @DeleteMapping("/documents/{processInstanceId}/grids/{gridName}")
    public ResponseEntity<Void> deleteGridRows(
            @PathVariable String processInstanceId,
            @PathVariable String gridName,
            @RequestParam String processDefinitionKey) {

        businessTableService.saveGridRows(processInstanceId, processDefinitionKey, gridName, null);
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
        private Map<String, Object> variables;
        private String userId;
    }

    @lombok.Data
    public static class SaveGridRowsRequest {
        private String processDefinitionKey;
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
        private Map<String, Object> variables;
        private String userId;
    }
}
