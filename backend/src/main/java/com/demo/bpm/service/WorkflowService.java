package com.demo.bpm.service;

import com.demo.bpm.dto.*;
import com.demo.bpm.exception.ResourceNotFoundException;
import com.demo.bpm.util.EscalationUtils;
import com.demo.bpm.util.WorkflowVariableUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowService {

    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final ObjectMapper objectMapper;

    @Transactional
    public EscalationDTO escalateTask(String taskId, EscalationRequest request, String userId) {
        Task task = getTaskOrThrow(taskId);

        Map<String, Object> variables = runtimeService.getVariables(task.getProcessInstanceId());
        String currentLevel = WorkflowVariableUtils.getStringVariable(variables, "currentLevel", "SUPERVISOR");

        String targetLevel = request.getTargetLevel();
        if (targetLevel == null) {
            targetLevel = EscalationUtils.getDefaultNextLevel(currentLevel);
        }

        if (!EscalationUtils.canEscalate(currentLevel, targetLevel)) {
             throw new RuntimeException("Invalid escalation from " + currentLevel + " to " + targetLevel);
        }

        // Record escalation in history
        int escalationCount = WorkflowVariableUtils.getIntVariable(variables, "escalationCount", 0) + 1;
        List<Map<String, Object>> escalationHistory = WorkflowVariableUtils.getListVariable(variables, "escalationHistory", objectMapper);

        Map<String, Object> escalationRecord = new HashMap<>();
        escalationRecord.put("id", UUID.randomUUID().toString());
        escalationRecord.put("taskId", taskId);
        escalationRecord.put("fromLevel", currentLevel);
        escalationRecord.put("toLevel", targetLevel);
        escalationRecord.put("fromUserId", userId);
        escalationRecord.put("reason", request.getReason());
        escalationRecord.put("type", "ESCALATE");
        escalationRecord.put("timestamp", LocalDateTime.now().toString());
        escalationHistory.add(escalationRecord);

        // Update process variables
        Map<String, Object> updateVars = new HashMap<>();
        updateVars.put("currentLevel", targetLevel);
        updateVars.put("escalationCount", escalationCount);
        updateVars.put("escalationHistory", WorkflowVariableUtils.serializeList(escalationHistory, objectMapper));
        updateVars.put("decision", "escalate");
        updateVars.put("escalationReason", request.getReason());
        updateVars.put("escalatedBy", userId);
        updateVars.put("escalatedAt", LocalDateTime.now().toString());

        // Complete current task with escalation decision
        taskService.complete(taskId, updateVars);

        log.info("Task {} escalated from {} to {} by {}", taskId, currentLevel, targetLevel, userId);

        return EscalationDTO.builder()
                .id((String) escalationRecord.get("id"))
                .taskId(taskId)
                .processInstanceId(task.getProcessInstanceId())
                .fromUserId(userId)
                .fromLevel(currentLevel)
                .toLevel(targetLevel)
                .reason(request.getReason())
                .type("ESCALATE")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Transactional
    public EscalationDTO deEscalateTask(String taskId, EscalationRequest request, String userId) {
        Task task = getTaskOrThrow(taskId);

        Map<String, Object> variables = runtimeService.getVariables(task.getProcessInstanceId());
        String currentLevel = WorkflowVariableUtils.getStringVariable(variables, "currentLevel", "SUPERVISOR");

        String targetLevel = request.getTargetLevel();
        if (targetLevel == null) {
            targetLevel = EscalationUtils.getDefaultPreviousLevel(currentLevel);
        }

        if (!EscalationUtils.canDeEscalate(currentLevel, targetLevel)) {
            throw new RuntimeException("Invalid de-escalation from " + currentLevel + " to " + targetLevel);
        }

        // Record de-escalation in history
        List<Map<String, Object>> escalationHistory = WorkflowVariableUtils.getListVariable(variables, "escalationHistory", objectMapper);

        Map<String, Object> deEscalationRecord = new HashMap<>();
        deEscalationRecord.put("id", UUID.randomUUID().toString());
        deEscalationRecord.put("taskId", taskId);
        deEscalationRecord.put("fromLevel", currentLevel);
        deEscalationRecord.put("toLevel", targetLevel);
        deEscalationRecord.put("fromUserId", userId);
        deEscalationRecord.put("reason", request.getReason());
        deEscalationRecord.put("type", "DE_ESCALATE");
        deEscalationRecord.put("timestamp", LocalDateTime.now().toString());
        escalationHistory.add(deEscalationRecord);

        // Update process variables
        Map<String, Object> updateVars = new HashMap<>();
        updateVars.put("currentLevel", targetLevel);
        updateVars.put("escalationHistory", WorkflowVariableUtils.serializeList(escalationHistory, objectMapper));
        updateVars.put("decision", "de_escalate");
        updateVars.put("deEscalationReason", request.getReason());
        updateVars.put("deEscalatedBy", userId);
        updateVars.put("deEscalatedAt", LocalDateTime.now().toString());

        // Complete current task with de-escalation decision
        taskService.complete(taskId, updateVars);

        log.info("Task {} de-escalated from {} to {} by {}", taskId, currentLevel, targetLevel, userId);

        return EscalationDTO.builder()
                .id((String) deEscalationRecord.get("id"))
                .taskId(taskId)
                .processInstanceId(task.getProcessInstanceId())
                .fromUserId(userId)
                .fromLevel(currentLevel)
                .toLevel(targetLevel)
                .reason(request.getReason())
                .type("DE_ESCALATE")
                .timestamp(LocalDateTime.now())
                .build();
    }

    public List<String> getEscalationOptions(String taskId) {
        Task task = getTaskOrThrow(taskId);

        Map<String, Object> variables = runtimeService.getVariables(task.getProcessInstanceId());
        String currentLevel = WorkflowVariableUtils.getStringVariable(variables, "currentLevel", "SUPERVISOR");

        return EscalationUtils.getNextLevels(currentLevel);
    }

    public List<String> getDeEscalationOptions(String taskId) {
        Task task = getTaskOrThrow(taskId);

        Map<String, Object> variables = runtimeService.getVariables(task.getProcessInstanceId());
        String currentLevel = WorkflowVariableUtils.getStringVariable(variables, "currentLevel", "SUPERVISOR");

        return EscalationUtils.getPreviousLevels(currentLevel);
    }

    private Task getTaskOrThrow(String taskId) {
        Task task = taskService.createTaskQuery()
                .taskId(taskId)
                .singleResult();
        if (task == null) {
            throw new ResourceNotFoundException("Task not found: " + taskId);
        }
        return task;
    }

    @Transactional
    public void handoffTask(String taskId, String toUserId, String reason, String fromUserId) {
        Task task = taskService.createTaskQuery()
                .taskId(taskId)
                .singleResult();

        if (task == null) {
            throw new ResourceNotFoundException("Task not found: " + taskId);
        }

        Map<String, Object> variables = runtimeService.getVariables(task.getProcessInstanceId());
        List<Map<String, Object>> handoffHistory = WorkflowVariableUtils.getListVariable(variables, "handoffHistory", objectMapper);

        Map<String, Object> handoffRecord = new HashMap<>();
        handoffRecord.put("id", UUID.randomUUID().toString());
        handoffRecord.put("taskId", taskId);
        handoffRecord.put("taskName", task.getName());
        handoffRecord.put("fromUserId", fromUserId);
        handoffRecord.put("toUserId", toUserId);
        handoffRecord.put("reason", reason);
        handoffRecord.put("timestamp", LocalDateTime.now().toString());
        handoffHistory.add(handoffRecord);

        runtimeService.setVariable(task.getProcessInstanceId(), "handoffHistory", WorkflowVariableUtils.serializeList(handoffHistory, objectMapper));

        // Unclaim and reassign
        if (task.getAssignee() != null) {
            taskService.unclaim(taskId);
        }
        taskService.claim(taskId, toUserId);

        log.info("Task {} handed off from {} to {} - Reason: {}", taskId, fromUserId, toUserId, reason);
    }

    @Transactional
    public ApprovalDTO recordApproval(String taskId, String decision, String comments, String userId) {
        Task task = taskService.createTaskQuery()
                .taskId(taskId)
                .singleResult();

        if (task == null) {
            throw new ResourceNotFoundException("Task not found: " + taskId);
        }

        Map<String, Object> variables = runtimeService.getVariables(task.getProcessInstanceId());
        String currentLevel = WorkflowVariableUtils.getStringVariable(variables, "currentLevel", "SUPERVISOR");

        List<Map<String, Object>> approvalHistory = WorkflowVariableUtils.getListVariable(variables, "approvalHistory", objectMapper);

        Map<String, Object> approvalRecord = new HashMap<>();
        approvalRecord.put("id", UUID.randomUUID().toString());
        approvalRecord.put("taskId", taskId);
        approvalRecord.put("taskName", task.getName());
        approvalRecord.put("approverId", userId);
        approvalRecord.put("approverLevel", currentLevel);
        approvalRecord.put("decision", decision);
        approvalRecord.put("comments", comments);
        approvalRecord.put("timestamp", LocalDateTime.now().toString());
        approvalRecord.put("stepOrder", approvalHistory.size() + 1);
        approvalHistory.add(approvalRecord);

        // Update variables
        Map<String, Object> updateVars = new HashMap<>();
        updateVars.put("approvalHistory", WorkflowVariableUtils.serializeList(approvalHistory, objectMapper));
        updateVars.put("decision", decision.toLowerCase());
        updateVars.put("approvalComments", comments);
        updateVars.put("completedBy", userId);

        taskService.complete(taskId, updateVars);

        log.info("Approval recorded for task {} by {} - Decision: {}", taskId, userId, decision);

        return ApprovalDTO.builder()
                .id((String) approvalRecord.get("id"))
                .processInstanceId(task.getProcessInstanceId())
                .taskId(taskId)
                .taskName(task.getName())
                .approverId(userId)
                .approverLevel(currentLevel)
                .decision(decision)
                .comments(comments)
                .timestamp(LocalDateTime.now())
                .stepOrder(approvalHistory.size())
                .isRequired(true)
                .build();
    }
}
