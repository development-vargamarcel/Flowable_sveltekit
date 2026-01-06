package com.demo.bpm.controller;

import com.demo.bpm.dto.FormDefinitionDTO;
import com.demo.bpm.dto.ProcessDTO;
import com.demo.bpm.dto.ProcessInstanceDTO;
import com.demo.bpm.dto.StartProcessRequest;
import com.demo.bpm.service.FormDefinitionService;
import com.demo.bpm.service.ProcessService;
import com.demo.bpm.service.UserService;
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

    @GetMapping
    public ResponseEntity<List<ProcessDTO>> getAvailableProcesses() {
        // Return active latest processes for starting new instances
        List<ProcessDTO> processes = processService.getAvailableProcesses();
        return ResponseEntity.ok(processes);
    }

    @GetMapping("/definitions")
    public ResponseEntity<List<ProcessDTO>> getAllProcessDefinitions() {
        // Return ALL definitions for management
        List<ProcessDTO> processes = processService.getAllProcessDefinitions();
        return ResponseEntity.ok(processes);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProcessDTO> getProcessById(@PathVariable String id) {
        return ResponseEntity.ok(processService.getProcessById(id));
    }

    @PostMapping("/{processKey}/start")
    public ResponseEntity<?> startProcess(
            @PathVariable String processKey,
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

    @GetMapping("/instance/{processInstanceId}")
    public ResponseEntity<?> getProcessInstance(@PathVariable String processInstanceId) {
        try {
            ProcessInstanceDTO instance = processService.getProcessInstance(processInstanceId);
            return ResponseEntity.ok(instance);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/my-processes")
    public ResponseEntity<Page<ProcessInstanceDTO>> getMyProcesses(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ProcessInstanceDTO> processes = processService.getActiveProcesses(userDetails.getUsername(), pageable);
        return ResponseEntity.ok(processes);
    }

    @GetMapping("/users")
    public ResponseEntity<?> getUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

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

    @GetMapping("/{processDefinitionId}/bpmn")
    public ResponseEntity<?> getProcessBpmn(@PathVariable String processDefinitionId) {
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

    @DeleteMapping("/{processDefinitionId}")
    public ResponseEntity<?> deleteProcess(
            @PathVariable String processDefinitionId,
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

    @PutMapping("/{processDefinitionId}/suspend")
    public ResponseEntity<?> suspendProcess(@PathVariable String processDefinitionId) {
        try {
            processService.suspendProcessDefinition(processDefinitionId);
            return ResponseEntity.ok(Map.of("message", "Process definition suspended"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{processDefinitionId}/activate")
    public ResponseEntity<?> activateProcess(@PathVariable String processDefinitionId) {
        try {
            processService.activateProcessDefinition(processDefinitionId);
            return ResponseEntity.ok(Map.of("message", "Process definition activated"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{processDefinitionId}/category")
    public ResponseEntity<?> updateCategory(
            @PathVariable String processDefinitionId,
            @RequestBody Map<String, String> body) {
        try {
            String category = body.get("category");
            processService.updateProcessDefinitionCategory(processDefinitionId, category);
            return ResponseEntity.ok(Map.of("message", "Category updated"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{processDefinitionId}/start-form")
    public ResponseEntity<?> getStartFormDefinition(@PathVariable String processDefinitionId) {
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

    @GetMapping("/{processDefinitionId}/forms")
    public ResponseEntity<?> getAllFormDefinitions(@PathVariable String processDefinitionId) {
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

    @GetMapping("/{processDefinitionId}/forms/{elementId}")
    public ResponseEntity<?> getFormDefinitionForElement(
            @PathVariable String processDefinitionId,
            @PathVariable String elementId) {
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
}
