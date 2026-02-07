package com.demo.bpm.controller;

import com.demo.bpm.dto.*;
import com.demo.bpm.service.DashboardService;
import com.demo.bpm.service.WorkflowHistoryService;
import com.demo.bpm.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
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
@Validated
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

    @PostMapping("/processes/{processInstanceId}/comments")
    public ResponseEntity<?> addComment(
            @PathVariable String processInstanceId,
            @Valid @RequestBody AddCommentRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        CommentDTO comment = workflowService.addComment(processInstanceId, request.getMessage(), userDetails.getUsername());

        return ResponseEntity.ok(Map.of(
                "message", "Comment added successfully",
                "comment", comment
        ));
    }

    // ==================== Escalation ====================

    @PostMapping("/tasks/{taskId}/escalate")
    public ResponseEntity<?> escalateTask(
            @PathVariable String taskId,
            @Valid @RequestBody EscalationRequest request,
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
            @Valid @RequestBody EscalationRequest request,
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
            @Valid @RequestBody HandoffRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        workflowService.handoffTask(taskId, request.getToUserId(), request.getReason(), userDetails.getUsername());
        return ResponseEntity.ok(Map.of(
                "message", "Task handed off successfully to " + request.getToUserId()
        ));
    }

    // ==================== Approvals ====================

    @PostMapping("/tasks/{taskId}/approve")
    public ResponseEntity<?> approveTask(
            @PathVariable String taskId,
            @Valid @RequestBody ApprovalRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        ApprovalDTO approval = workflowService.recordApproval(taskId, "APPROVED", request.getComments(), userDetails.getUsername());
        return ResponseEntity.ok(Map.of(
                "message", "Task approved successfully",
                "approval", approval
        ));
    }

    @PostMapping("/tasks/{taskId}/reject")
    public ResponseEntity<?> rejectTask(
            @PathVariable String taskId,
            @Valid @RequestBody ApprovalRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        ApprovalDTO approval = workflowService.recordApproval(taskId, "REJECTED", request.getComments(), userDetails.getUsername());
        return ResponseEntity.ok(Map.of(
                "message", "Task rejected",
                "approval", approval
        ));
    }

    @PostMapping("/tasks/{taskId}/request-changes")
    public ResponseEntity<?> requestChanges(
            @PathVariable String taskId,
            @Valid @RequestBody ApprovalRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        ApprovalDTO approval = workflowService.recordApproval(taskId, "REQUEST_CHANGES", request.getComments(), userDetails.getUsername());
        return ResponseEntity.ok(Map.of(
                "message", "Changes requested",
                "approval", approval
        ));
    }

    // Request DTOs
    @lombok.Data
    public static class AddCommentRequest {
        @NotBlank(message = "Comment message is required")
        private String message;
    }

    @lombok.Data
    public static class HandoffRequest {
        @NotBlank(message = "Target user is required")
        private String toUserId;
        private String reason;
    }

    @lombok.Data
    public static class ApprovalRequest {
        @NotBlank(message = "Comments are required")
        private String comments;
    }
}
