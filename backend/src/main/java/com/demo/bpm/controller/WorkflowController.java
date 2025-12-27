package com.demo.bpm.controller;

import com.demo.bpm.dto.*;
import com.demo.bpm.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/workflow")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;

    // ==================== Dashboard ====================

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardDTO> getDashboard(
            @AuthenticationPrincipal UserDetails userDetails) {
        DashboardDTO dashboard = workflowService.getDashboard(userDetails.getUsername());
        return ResponseEntity.ok(dashboard);
    }

    // ==================== Process History ====================

    @GetMapping("/processes")
    public ResponseEntity<List<WorkflowHistoryDTO>> getAllProcesses(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String processType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<WorkflowHistoryDTO> processes = workflowService.getAllProcesses(status, processType, page, size);
        return ResponseEntity.ok(processes);
    }

    @GetMapping("/processes/{processInstanceId}")
    public ResponseEntity<?> getProcessHistory(@PathVariable String processInstanceId) {
        try {
            WorkflowHistoryDTO history = workflowService.getWorkflowHistory(processInstanceId);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/processes/{processInstanceId}/tasks")
    public ResponseEntity<List<TaskHistoryDTO>> getProcessTaskHistory(
            @PathVariable String processInstanceId) {
        List<TaskHistoryDTO> taskHistory = workflowService.getTaskHistory(processInstanceId);
        return ResponseEntity.ok(taskHistory);
    }

    // ==================== Escalation ====================

    @PostMapping("/tasks/{taskId}/escalate")
    public ResponseEntity<?> escalateTask(
            @PathVariable String taskId,
            @RequestBody EscalationRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            EscalationDTO escalation = workflowService.escalateTask(taskId, request, userDetails.getUsername());
            return ResponseEntity.ok(Map.of(
                    "message", "Task escalated successfully",
                    "escalation", escalation
            ));
        } catch (Exception e) {
            log.error("Error escalating task {}: {}", taskId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    @PostMapping("/tasks/{taskId}/de-escalate")
    public ResponseEntity<?> deEscalateTask(
            @PathVariable String taskId,
            @RequestBody EscalationRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            EscalationDTO deEscalation = workflowService.deEscalateTask(taskId, request, userDetails.getUsername());
            return ResponseEntity.ok(Map.of(
                    "message", "Task de-escalated successfully",
                    "deEscalation", deEscalation
            ));
        } catch (Exception e) {
            log.error("Error de-escalating task {}: {}", taskId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/tasks/{taskId}/escalation-options")
    public ResponseEntity<?> getEscalationOptions(@PathVariable String taskId) {
        try {
            List<String> escalateOptions = workflowService.getEscalationOptions(taskId);
            List<String> deEscalateOptions = workflowService.getDeEscalationOptions(taskId);
            return ResponseEntity.ok(Map.of(
                    "escalateTo", escalateOptions,
                    "deEscalateTo", deEscalateOptions
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    // ==================== Handoff ====================

    @PostMapping("/tasks/{taskId}/handoff")
    public ResponseEntity<?> handoffTask(
            @PathVariable String taskId,
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            String toUserId = request.get("toUserId");
            String reason = request.get("reason");

            if (toUserId == null || toUserId.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Target user is required"
                ));
            }

            workflowService.handoffTask(taskId, toUserId, reason, userDetails.getUsername());
            return ResponseEntity.ok(Map.of(
                    "message", "Task handed off successfully to " + toUserId
            ));
        } catch (Exception e) {
            log.error("Error handing off task {}: {}", taskId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    // ==================== Approvals ====================

    @PostMapping("/tasks/{taskId}/approve")
    public ResponseEntity<?> approveTask(
            @PathVariable String taskId,
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            String comments = request.get("comments");
            ApprovalDTO approval = workflowService.recordApproval(taskId, "APPROVED", comments, userDetails.getUsername());
            return ResponseEntity.ok(Map.of(
                    "message", "Task approved successfully",
                    "approval", approval
            ));
        } catch (Exception e) {
            log.error("Error approving task {}: {}", taskId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    @PostMapping("/tasks/{taskId}/reject")
    public ResponseEntity<?> rejectTask(
            @PathVariable String taskId,
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            String comments = request.get("comments");
            if (comments == null || comments.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Rejection reason is required"
                ));
            }
            ApprovalDTO approval = workflowService.recordApproval(taskId, "REJECTED", comments, userDetails.getUsername());
            return ResponseEntity.ok(Map.of(
                    "message", "Task rejected",
                    "approval", approval
            ));
        } catch (Exception e) {
            log.error("Error rejecting task {}: {}", taskId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    @PostMapping("/tasks/{taskId}/request-changes")
    public ResponseEntity<?> requestChanges(
            @PathVariable String taskId,
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            String comments = request.get("comments");
            if (comments == null || comments.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Change request details are required"
                ));
            }
            ApprovalDTO approval = workflowService.recordApproval(taskId, "REQUEST_CHANGES", comments, userDetails.getUsername());
            return ResponseEntity.ok(Map.of(
                    "message", "Changes requested",
                    "approval", approval
            ));
        } catch (Exception e) {
            log.error("Error requesting changes for task {}: {}", taskId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }
}
