package com.demo.bpm.service;

import com.demo.bpm.dto.*;
import com.demo.bpm.exception.ResourceNotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
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

    private static final Map<String, String[]> ESCALATION_HIERARCHY = Map.of(
            "SUPERVISOR", new String[]{"MANAGER"},
            "MANAGER", new String[]{"DIRECTOR"},
            "DIRECTOR", new String[]{"EXECUTIVE"},
            "EXECUTIVE", new String[]{}
    );

    private static final Map<String, String[]> DE_ESCALATION_HIERARCHY = Map.of(
            "EXECUTIVE", new String[]{"DIRECTOR"},
            "DIRECTOR", new String[]{"MANAGER"},
            "MANAGER", new String[]{"SUPERVISOR"},
            "SUPERVISOR", new String[]{}
    );

    @Transactional
    public EscalationDTO escalateTask(String taskId, EscalationRequest request, String userId) {
        Task task = taskService.createTaskQuery()
                .taskId(taskId)
                .singleResult();

        if (task == null) {
            throw new ResourceNotFoundException("Task not found: " + taskId);
        }

        Map<String, Object> variables = runtimeService.getVariables(task.getProcessInstanceId());
        String currentLevel = (String) variables.getOrDefault("currentLevel", "SUPERVISOR");

        String[] nextLevels = ESCALATION_HIERARCHY.getOrDefault(currentLevel, new String[]{});
        if (nextLevels.length == 0) {
            throw new RuntimeException("Cannot escalate beyond " + currentLevel);
        }

        String targetLevel = request.getTargetLevel() != null ? request.getTargetLevel() : nextLevels[0];

        // Validate target level is in hierarchy
        if (!Arrays.asList(nextLevels).contains(targetLevel)) {
            throw new RuntimeException("Invalid escalation target: " + targetLevel);
        }

        // Record escalation in history
        int escalationCount = ((Number) variables.getOrDefault("escalationCount", 0)).intValue() + 1;
        List<Map<String, Object>> escalationHistory = getEscalationHistoryList(variables);

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
        updateVars.put("escalationHistory", serializeList(escalationHistory));
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
        Task task = taskService.createTaskQuery()
                .taskId(taskId)
                .singleResult();

        if (task == null) {
            throw new RuntimeException("Task not found: " + taskId);
        }

        Map<String, Object> variables = runtimeService.getVariables(task.getProcessInstanceId());
        String currentLevel = (String) variables.getOrDefault("currentLevel", "SUPERVISOR");

        String[] prevLevels = DE_ESCALATION_HIERARCHY.getOrDefault(currentLevel, new String[]{});
        if (prevLevels.length == 0) {
            throw new RuntimeException("Cannot de-escalate below " + currentLevel);
        }

        String targetLevel = request.getTargetLevel() != null ? request.getTargetLevel() : prevLevels[0];

        // Record de-escalation in history
        List<Map<String, Object>> escalationHistory = getEscalationHistoryList(variables);

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
        updateVars.put("escalationHistory", serializeList(escalationHistory));
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
        Task task = taskService.createTaskQuery()
                .taskId(taskId)
                .singleResult();

        if (task == null) {
            throw new RuntimeException("Task not found: " + taskId);
        }

        Map<String, Object> variables = runtimeService.getVariables(task.getProcessInstanceId());
        String currentLevel = (String) variables.getOrDefault("currentLevel", "SUPERVISOR");

        return Arrays.asList(ESCALATION_HIERARCHY.getOrDefault(currentLevel, new String[]{}));
    }

    public List<String> getDeEscalationOptions(String taskId) {
        Task task = taskService.createTaskQuery()
                .taskId(taskId)
                .singleResult();

        if (task == null) {
            throw new RuntimeException("Task not found: " + taskId);
        }

        Map<String, Object> variables = runtimeService.getVariables(task.getProcessInstanceId());
        String currentLevel = (String) variables.getOrDefault("currentLevel", "SUPERVISOR");

        return Arrays.asList(DE_ESCALATION_HIERARCHY.getOrDefault(currentLevel, new String[]{}));
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
        List<Map<String, Object>> handoffHistory = getHandoffHistoryList(variables);

        Map<String, Object> handoffRecord = new HashMap<>();
        handoffRecord.put("id", UUID.randomUUID().toString());
        handoffRecord.put("taskId", taskId);
        handoffRecord.put("taskName", task.getName());
        handoffRecord.put("fromUserId", fromUserId);
        handoffRecord.put("toUserId", toUserId);
        handoffRecord.put("reason", reason);
        handoffRecord.put("timestamp", LocalDateTime.now().toString());
        handoffHistory.add(handoffRecord);

        runtimeService.setVariable(task.getProcessInstanceId(), "handoffHistory", serializeList(handoffHistory));

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
        String currentLevel = (String) variables.getOrDefault("currentLevel", "SUPERVISOR");

        List<Map<String, Object>> approvalHistory = getApprovalHistoryList(variables);

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
        updateVars.put("approvalHistory", serializeList(approvalHistory));
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

    // Helper methods

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getEscalationHistoryList(Map<String, Object> variables) {
        Object historyObj = variables.get("escalationHistory");
        if (historyObj == null) {
            return new ArrayList<>();
        }
        if (historyObj instanceof List) {
            return new ArrayList<>((List<Map<String, Object>>) historyObj);
        }
        try {
            return objectMapper.readValue((String) historyObj,
                    new TypeReference<List<Map<String, Object>>>() {});
        } catch (JsonProcessingException e) {
            return new ArrayList<>();
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getHandoffHistoryList(Map<String, Object> variables) {
        Object historyObj = variables.get("handoffHistory");
        if (historyObj == null) {
            return new ArrayList<>();
        }
        if (historyObj instanceof List) {
            return new ArrayList<>((List<Map<String, Object>>) historyObj);
        }
        try {
            return objectMapper.readValue((String) historyObj,
                    new TypeReference<List<Map<String, Object>>>() {});
        } catch (JsonProcessingException e) {
            return new ArrayList<>();
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getApprovalHistoryList(Map<String, Object> variables) {
        Object historyObj = variables.get("approvalHistory");
        if (historyObj == null) {
            return new ArrayList<>();
        }
        if (historyObj instanceof List) {
            return new ArrayList<>((List<Map<String, Object>>) historyObj);
        }
        try {
            return objectMapper.readValue((String) historyObj,
                    new TypeReference<List<Map<String, Object>>>() {});
        } catch (JsonProcessingException e) {
            return new ArrayList<>();
        }
    }

    private String serializeList(List<Map<String, Object>> list) {
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }
}
