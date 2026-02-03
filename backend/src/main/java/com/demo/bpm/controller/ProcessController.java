package com.demo.bpm.controller;

import com.demo.bpm.dto.FormDefinitionDTO;
import com.demo.bpm.dto.ProcessDTO;
import com.demo.bpm.dto.ProcessInstanceDTO;
import com.demo.bpm.dto.StartProcessRequest;
import com.demo.bpm.exception.ResourceNotFoundException;
import com.demo.bpm.service.ExportService;
import com.demo.bpm.service.FormDefinitionService;
import com.demo.bpm.service.ProcessService;
import com.demo.bpm.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/processes")
@RequiredArgsConstructor
public class ProcessController {

    private final ProcessService processService;
    private final UserService userService;
    private final FormDefinitionService formDefinitionService;
    private final ExportService exportService;

    @Operation(summary = "Get available processes for starting")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Found the processes",
                    content = { @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = ProcessDTO.class))) }) })
    @GetMapping
    public ResponseEntity<List<ProcessDTO>> getAvailableProcesses() {
        // Return active latest processes for starting new instances
        List<ProcessDTO> processes = processService.getAvailableProcesses();
        return ResponseEntity.ok(processes);
    }

    @Operation(summary = "Get all process definitions")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Found all process definitions",
                    content = { @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = ProcessDTO.class))) }) })
    @GetMapping("/definitions")
    public ResponseEntity<List<ProcessDTO>> getAllProcessDefinitions() {
        // Return ALL definitions for management
        List<ProcessDTO> processes = processService.getAllProcessDefinitions();
        return ResponseEntity.ok(processes);
    }

    @Operation(summary = "Get a process definition by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Found the process definition",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ProcessDTO.class)) }) })
    @GetMapping("/{id}")
    public ResponseEntity<ProcessDTO> getProcessById(@Parameter(description = "ID of the process definition") @PathVariable String id) {
        return ResponseEntity.ok(processService.getProcessById(id));
    }

    @Operation(summary = "Start a process instance")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Process started successfully",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Map.class)) }),
            @ApiResponse(responseCode = "400", description = "Invalid request",
                    content = @Content) })
    @PostMapping("/{processKey}/start")
    public ResponseEntity<?> startProcess(
            @Parameter(description = "Key of the process to start") @PathVariable String processKey,
            @RequestBody(required = false) StartProcessRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            String businessKey = request != null ? request.getBusinessKey() : null;
            Map<String, Object> variables = request != null ? request.getVariables() : Map.of();

            // Add user info to variables
            var userInfo = userService.getUserInfo(userDetails);
            variables.put("employeeId", userDetails.getUsername());
            variables.put("employeeName", userInfo.getDisplayName());

            ProcessInstanceDTO instance = processService.startProcess(
                    processKey,
                    businessKey,
                    variables,
                    userDetails.getUsername()
            );

            return ResponseEntity.ok(Map.of(
                    "message", "Process started successfully",
                    "processInstance", instance
            ));
        } catch (Exception e) {
            log.error("Error starting process {}: {}", processKey, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    @Operation(summary = "Get a process instance by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Found the process instance",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ProcessInstanceDTO.class)) }),
            @ApiResponse(responseCode = "404", description = "Process instance not found",
                    content = @Content) })
    @GetMapping("/instance/{processInstanceId}")
    public ResponseEntity<?> getProcessInstance(@Parameter(description = "ID of the process instance") @PathVariable String processInstanceId) {
        try {
            ProcessInstanceDTO instance = processService.getProcessInstance(processInstanceId);
            return ResponseEntity.ok(instance);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Export process instance details")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Found the process instance",
                    content = { @Content(mediaType = "text/csv") }),
            @ApiResponse(responseCode = "404", description = "Process instance not found",
                    content = @Content) })
    @GetMapping("/instance/{processInstanceId}/export")
    public ResponseEntity<byte[]> exportProcessInstance(@Parameter(description = "ID of the process instance") @PathVariable String processInstanceId) {
        byte[] csvData = exportService.exportProcessInstance(processInstanceId);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"process_export_" + processInstanceId + ".csv\"")
                .header("Content-Type", "text/csv")
                .body(csvData);
    }

    @Operation(summary = "Get active processes for the current user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Found the active processes",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Page.class)) }) })
    @GetMapping("/my-processes")
    public ResponseEntity<Page<ProcessInstanceDTO>> getMyProcesses(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ProcessInstanceDTO> processes = processService.getActiveProcesses(userDetails.getUsername(), pageable);
        return ResponseEntity.ok(processes);
    }

    @Operation(summary = "Get all users")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Found the users",
                    content = { @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = Map.class))) }) })
    @GetMapping("/users")
    public ResponseEntity<?> getUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @Operation(summary = "Deploy a new process definition")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Process deployed successfully",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Map.class)) }),
            @ApiResponse(responseCode = "400", description = "Invalid request",
                    content = @Content) })
    @PostMapping("/deploy")
    public ResponseEntity<?> deployProcess(@RequestBody Map<String, String> request) {
        try {
            String processName = request.get("processName");
            String bpmnXml = request.get("bpmnXml");

            if (processName == null || processName.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Process name is required"
                ));
            }

            if (bpmnXml == null || bpmnXml.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "BPMN XML is required"
                ));
            }

            ProcessDTO deployedProcess = processService.deployProcess(processName, bpmnXml);

            return ResponseEntity.ok(Map.of(
                    "message", "Process deployed successfully",
                    "process", deployedProcess
            ));
        } catch (Exception e) {
            log.error("Error deploying process: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    @Operation(summary = "Get BPMN XML for a process definition")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Found the BPMN XML",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Map.class)) }),
            @ApiResponse(responseCode = "400", description = "Invalid request",
                    content = @Content) })
    @GetMapping("/{processDefinitionId}/bpmn")
    public ResponseEntity<?> getProcessBpmn(@Parameter(description = "ID of the process definition") @PathVariable String processDefinitionId) {
        try {
            String bpmnXml = processService.getProcessDefinitionBpmn(processDefinitionId);
            return ResponseEntity.ok(Map.of(
                    "bpmn", bpmnXml
            ));
        } catch (Exception e) {
            log.error("Error retrieving BPMN: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    @Operation(summary = "Delete a process definition")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Process definition deleted successfully",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Map.class)) }),
            @ApiResponse(responseCode = "400", description = "Invalid request",
                    content = @Content) })
    @DeleteMapping("/{processDefinitionId}")
    public ResponseEntity<?> deleteProcess(
            @Parameter(description = "ID of the process definition") @PathVariable String processDefinitionId,
            @RequestParam(defaultValue = "false") boolean cascade) {
        try {
            processService.deleteProcessDefinition(processDefinitionId, cascade);
            return ResponseEntity.ok(Map.of(
                    "message", "Process definition deleted successfully"
            ));
        } catch (Exception e) {
            log.error("Error deleting process: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    @Operation(summary = "Suspend a process definition")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Process definition suspended",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Map.class)) }),
            @ApiResponse(responseCode = "400", description = "Invalid request",
                    content = @Content) })
    @PutMapping("/{processDefinitionId}/suspend")
    public ResponseEntity<?> suspendProcess(@Parameter(description = "ID of the process definition") @PathVariable String processDefinitionId) {
        try {
            processService.suspendProcessDefinition(processDefinitionId);
            return ResponseEntity.ok(Map.of("message", "Process definition suspended"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Activate a process definition")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Process definition activated",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Map.class)) }),
            @ApiResponse(responseCode = "400", description = "Invalid request",
                    content = @Content) })
    @PutMapping("/{processDefinitionId}/activate")
    public ResponseEntity<?> activateProcess(@Parameter(description = "ID of the process definition") @PathVariable String processDefinitionId) {
        try {
            processService.activateProcessDefinition(processDefinitionId);
            return ResponseEntity.ok(Map.of("message", "Process definition activated"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Update process definition category")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Category updated",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Map.class)) }),
            @ApiResponse(responseCode = "400", description = "Invalid request",
                    content = @Content) })
    @PutMapping("/{processDefinitionId}/category")
    public ResponseEntity<?> updateCategory(
            @Parameter(description = "ID of the process definition") @PathVariable String processDefinitionId,
            @RequestBody Map<String, String> body) {
        try {
            String category = body.get("category");
            processService.updateProcessDefinitionCategory(processDefinitionId, category);
            return ResponseEntity.ok(Map.of("message", "Category updated"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Get start form definition")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Found the start form definition",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = FormDefinitionDTO.class)) }),
            @ApiResponse(responseCode = "400", description = "Invalid request",
                    content = @Content) })
    @GetMapping("/{processDefinitionId}/start-form")
    public ResponseEntity<?> getStartFormDefinition(@Parameter(description = "ID of the process definition") @PathVariable String processDefinitionId) {
        try {
            FormDefinitionDTO formDefinition = formDefinitionService.getStartFormDefinition(processDefinitionId);
            return ResponseEntity.ok(formDefinition);
        } catch (Exception e) {
            log.error("Error retrieving start form definition: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    @Operation(summary = "Get all form definitions for a process")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Found the form definitions",
                    content = { @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = FormDefinitionDTO.class))) }),
            @ApiResponse(responseCode = "400", description = "Invalid request",
                    content = @Content) })
    @GetMapping("/{processDefinitionId}/forms")
    public ResponseEntity<?> getAllFormDefinitions(@Parameter(description = "ID of the process definition") @PathVariable String processDefinitionId) {
        try {
            var formDefinitions = formDefinitionService.getAllFormDefinitions(processDefinitionId);
            return ResponseEntity.ok(formDefinitions);
        } catch (Exception e) {
            log.error("Error retrieving form definitions: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    @Operation(summary = "Get form definition for a specific element")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Found the form definition",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = FormDefinitionDTO.class)) }),
            @ApiResponse(responseCode = "400", description = "Invalid request",
                    content = @Content) })
    @GetMapping("/{processDefinitionId}/forms/{elementId}")
    public ResponseEntity<?> getFormDefinitionForElement(
            @Parameter(description = "ID of the process definition") @PathVariable String processDefinitionId,
            @Parameter(description = "Element ID (e.g. task ID)") @PathVariable String elementId) {
        try {
            FormDefinitionDTO formDefinition = formDefinitionService.getFormDefinitionForElement(
                    processDefinitionId, elementId);
            return ResponseEntity.ok(formDefinition);
        } catch (Exception e) {
            log.error("Error retrieving form definition for element: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    @Operation(summary = "Cancel a process instance")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Process instance cancelled successfully",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Map.class)) }),
            @ApiResponse(responseCode = "400", description = "Invalid request or unauthorized",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Process instance not found",
                    content = @Content) })
    @DeleteMapping("/instance/{processInstanceId}")
    public ResponseEntity<?> cancelProcessInstance(
            @Parameter(description = "ID of the process instance") @PathVariable String processInstanceId,
            @RequestParam(required = false) String reason,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            boolean isAdmin = userDetails.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

            processService.cancelProcessInstance(processInstanceId, reason, userDetails.getUsername(), isAdmin);

            return ResponseEntity.ok(Map.of("message", "Process instance cancelled successfully"));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error cancelling process instance: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
