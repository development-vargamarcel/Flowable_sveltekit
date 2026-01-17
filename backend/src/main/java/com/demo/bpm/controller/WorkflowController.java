package com.demo.bpm.controller;

import com.demo.bpm.dto.*;
import com.demo.bpm.service.DashboardService;
import com.demo.bpm.service.WorkflowHistoryService;
import com.demo.bpm.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@RequestMapping("/api/workflow")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;
    private final WorkflowHistoryService workflowHistoryService;
    private final DashboardService dashboardService;

    // ==================== Dashboard ====================

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardDTO> getDashboard(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type) {
        Pageable pageable = PageRequest.of(page, size);
        DashboardDTO dashboard = dashboardService.getDashboard(userDetails.getUsername(), pageable, status, type);
        return ResponseEntity.ok(dashboard);
    }

    // ==================== Process History ====================

    @GetMapping("/processes")
    public ResponseEntity<List<WorkflowHistoryDTO>> getAllProcesses(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String processType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<WorkflowHistoryDTO> processes = workflowHistoryService.getAllProcesses(status, processType, page, size);
        return ResponseEntity.ok(processes);
    }

    @GetMapping("/processes/{processInstanceId}")
    public ResponseEntity<WorkflowHistoryDTO> getProcessHistory(@PathVariable String processInstanceId) {
        WorkflowHistoryDTO history = workflowHistoryService.getWorkflowHistory(processInstanceId);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/processes/{processInstanceId}/tasks")
    public ResponseEntity<List<TaskHistoryDTO>> getProcessTaskHistory(
            @PathVariable String processInstanceId) {
        List<TaskHistoryDTO> taskHistory = workflowHistoryService.getTaskHistory(processInstanceId);
        return ResponseEntity.ok(taskHistory);
    }

    // ==================== Escalation ====================

    @PostMapping("/tasks/{taskId}/escalate")
    public ResponseEntity<?> escalateTask(
            @PathVariable String taskId,
            @RequestBody EscalationRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        EscalationDTO escalation = workflowService.escalateTask(taskId, request, userDetails.getUsername());
        return ResponseEntity.ok(Map.of(
                "message", "Task escalated successfully",
                "escalation", escalation
        ));
    }

    @PostMapping("/tasks/{taskId}/de-escalate")
    public ResponseEntity<?> deEscalateTask(
            @PathVariable String taskId,
            @RequestBody EscalationRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        EscalationDTO deEscalation = workflowService.deEscalateTask(taskId, request, userDetails.getUsername());
        return ResponseEntity.ok(Map.of(
                "message", "Task de-escalated successfully",
                "deEscalation", deEscalation
        ));
    }

    @GetMapping("/tasks/{taskId}/escalation-options")
    public ResponseEntity<?> getEscalationOptions(@PathVariable String taskId) {
        List<String> escalateOptions = workflowService.getEscalationOptions(taskId);
        List<String> deEscalateOptions = workflowService.getDeEscalationOptions(taskId);
        return ResponseEntity.ok(Map.of(
                "escalateTo", escalateOptions,
                "deEscalateTo", deEscalateOptions
        ));
    }

    // ==================== Handoff ====================

    @PostMapping("/tasks/{taskId}/handoff")
    public ResponseEntity<?> handoffTask(
            @PathVariable String taskId,
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal UserDetails userDetails) {
        String toUserId = request.get("toUserId");
        String reason = request.get("reason");

        if (toUserId == null || toUserId.isEmpty()) {
            throw new IllegalArgumentException("Target user is required");
        }

        workflowService.handoffTask(taskId, toUserId, reason, userDetails.getUsername());
        return ResponseEntity.ok(Map.of(
                "message", "Task handed off successfully to " + toUserId
        ));
    }

    // ==================== Approvals ====================

    @PostMapping("/tasks/{taskId}/approve")
    public ResponseEntity<?> approveTask(
            @PathVariable String taskId,
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal UserDetails userDetails) {
        String comments = request.get("comments");
        ApprovalDTO approval = workflowService.recordApproval(taskId, "APPROVED", comments, userDetails.getUsername());
        return ResponseEntity.ok(Map.of(
                "message", "Task approved successfully",
                "approval", approval
        ));
    }

    @PostMapping("/tasks/{taskId}/reject")
    public ResponseEntity<?> rejectTask(
            @PathVariable String taskId,
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal UserDetails userDetails) {
        String comments = request.get("comments");
        if (comments == null || comments.isEmpty()) {
            throw new IllegalArgumentException("Rejection reason is required");
        }
        ApprovalDTO approval = workflowService.recordApproval(taskId, "REJECTED", comments, userDetails.getUsername());
        return ResponseEntity.ok(Map.of(
                "message", "Task rejected",
                "approval", approval
        ));
    }

    @PostMapping("/tasks/{taskId}/request-changes")
    public ResponseEntity<?> requestChanges(
            @PathVariable String taskId,
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal UserDetails userDetails) {
        String comments = request.get("comments");
        if (comments == null || comments.isEmpty()) {
            throw new IllegalArgumentException("Change request details are required");
        }
        ApprovalDTO approval = workflowService.recordApproval(taskId, "REQUEST_CHANGES", comments, userDetails.getUsername());
        return ResponseEntity.ok(Map.of(
                "message", "Changes requested",
                "approval", approval
        ));
    }
}
