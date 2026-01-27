package com.demo.bpm.service;

import com.demo.bpm.dto.*;
import com.demo.bpm.exception.InvalidOperationException;
import com.demo.bpm.exception.ResourceNotFoundException;
import com.demo.bpm.service.helpers.HistoryRecorder;
import com.demo.bpm.util.EscalationUtils;
import com.demo.bpm.util.WorkflowConstants;
import com.demo.bpm.util.WorkflowVariableUtils;
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
    private final HistoryRecorder historyRecorder;

    /**
     * Escalates a task to the next level of authority.
     *
     * @param taskId the ID of the task to escalate
     * @param request the escalation request details
     * @param userId the ID of the user performing the escalation
     * @return the escalation result DTO
     */
    @Transactional
    public EscalationDTO escalateTask(String taskId, EscalationRequest request, String userId) {
        log.debug("Escalating task {} by user {}", taskId, userId);
        return processEscalation(taskId, request, userId, true);
    }

    /**
     * De-escalates a task to the previous level of authority.
     *
     * @param taskId the ID of the task to de-escalate
     * @param request the de-escalation request details
     * @param userId the ID of the user performing the de-escalation
     * @return the de-escalation result DTO
     */
    @Transactional
    public EscalationDTO deEscalateTask(String taskId, EscalationRequest request, String userId) {
        log.debug("De-escalating task {} by user {}", taskId, userId);
        return processEscalation(taskId, request, userId, false);
    }

    private EscalationDTO processEscalation(String taskId, EscalationRequest request, String userId, boolean isEscalation) {
        Task task = getTaskOrThrow(taskId);
        String processInstanceId = task.getProcessInstanceId();
        Map<String, Object> variables = runtimeService.getVariables(processInstanceId);
        String currentLevel = WorkflowVariableUtils.getStringVariable(variables, WorkflowConstants.VAR_CURRENT_LEVEL, WorkflowConstants.LEVEL_SUPERVISOR);

        String targetLevel = request.getTargetLevel();
        if (targetLevel == null) {
            targetLevel = isEscalation
                ? EscalationUtils.getDefaultNextLevel(currentLevel)
                : EscalationUtils.getDefaultPreviousLevel(currentLevel);
        }

        validateEscalation(currentLevel, targetLevel, isEscalation);

        String id = historyRecorder.recordEscalationHistory(
            processInstanceId, taskId, userId, currentLevel, targetLevel,
            request.getReason(), isEscalation, variables
        );

        updateVariablesForEscalation(processInstanceId, isEscalation, variables, targetLevel, request.getReason(), userId);

        taskService.complete(taskId);

        log.info("Task {} (ProcessInstance: {}) {} from {} to {} by {}", taskId, processInstanceId, isEscalation ? "escalated" : "de-escalated", currentLevel, targetLevel, userId);

        return EscalationDTO.builder()
                .id(id)
                .taskId(taskId)
                .processInstanceId(processInstanceId)
                .fromUserId(userId)
                .fromLevel(currentLevel)
                .toLevel(targetLevel)
                .reason(request.getReason())
                .type(isEscalation ? WorkflowConstants.TYPE_ESCALATE : WorkflowConstants.TYPE_DE_ESCALATE)
                .timestamp(LocalDateTime.now())
                .build();
    }

    private void validateEscalation(String currentLevel, String targetLevel, boolean isEscalation) {
        boolean valid = isEscalation
            ? EscalationUtils.canEscalate(currentLevel, targetLevel)
            : EscalationUtils.canDeEscalate(currentLevel, targetLevel);

        if (!valid) {
             throw new InvalidOperationException("Invalid " + (isEscalation ? "escalation" : "de-escalation") + " from " + currentLevel + " to " + targetLevel);
        }
    }

    private void updateVariablesForEscalation(String processInstanceId, boolean isEscalation, Map<String, Object> currentVariables, String targetLevel, String reason, String userId) {
        Map<String, Object> updateVars = new HashMap<>();
        updateVars.put(WorkflowConstants.VAR_CURRENT_LEVEL, targetLevel);

        String now = LocalDateTime.now().toString();

        if (isEscalation) {
            int count = WorkflowVariableUtils.getIntVariable(currentVariables, WorkflowConstants.VAR_ESCALATION_COUNT, 0) + 1;
            updateVars.put(WorkflowConstants.VAR_ESCALATION_COUNT, count);
            updateVars.put(WorkflowConstants.VAR_DECISION, "escalate");
            updateVars.put(WorkflowConstants.VAR_ESCALATION_REASON, reason);
            updateVars.put(WorkflowConstants.VAR_ESCALATED_BY, userId);
            updateVars.put(WorkflowConstants.VAR_ESCALATED_AT, now);
        } else {
            updateVars.put(WorkflowConstants.VAR_DECISION, "de_escalate");
            updateVars.put(WorkflowConstants.VAR_DE_ESCALATION_REASON, reason);
            updateVars.put(WorkflowConstants.VAR_DE_ESCALATED_BY, userId);
            updateVars.put(WorkflowConstants.VAR_DE_ESCALATED_AT, now);
        }

        runtimeService.setVariables(processInstanceId, updateVars);
    }

    public List<String> getEscalationOptions(String taskId) {
        Task task = getTaskOrThrow(taskId);
        String currentLevel = getCurrentLevel(task.getProcessInstanceId());
        return EscalationUtils.getNextLevels(currentLevel);
    }

    public List<String> getDeEscalationOptions(String taskId) {
        Task task = getTaskOrThrow(taskId);
        String currentLevel = getCurrentLevel(task.getProcessInstanceId());
        return EscalationUtils.getPreviousLevels(currentLevel);
    }

    private String getCurrentLevel(String processInstanceId) {
        return WorkflowVariableUtils.getStringVariable(
            runtimeService.getVariables(processInstanceId),
            WorkflowConstants.VAR_CURRENT_LEVEL,
            WorkflowConstants.LEVEL_SUPERVISOR
        );
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

    /**
     * Hands off a task from one user to another.
     *
     * @param taskId the ID of the task
     * @param toUserId the ID of the user receiving the task
     * @param reason the reason for the handoff
     * @param fromUserId the ID of the user handing off the task
     */
    @Transactional
    public void handoffTask(String taskId, String toUserId, String reason, String fromUserId) {
        log.debug("Handing off task {} from {} to {} due to: {}", taskId, fromUserId, toUserId, reason);
        Task task = getTaskOrThrow(taskId);
        String processInstanceId = task.getProcessInstanceId();

        historyRecorder.recordHandoffHistory(processInstanceId, taskId, task.getName(),
            fromUserId, toUserId, reason, runtimeService.getVariables(processInstanceId));

        // Unclaim and reassign
        if (task.getAssignee() != null) {
            taskService.unclaim(taskId);
        }
        taskService.claim(taskId, toUserId);

        log.info("Task {} (ProcessInstance: {}) handed off from {} to {} - Reason: {}", taskId, processInstanceId, fromUserId, toUserId, reason);
    }

    /**
     * Records an approval decision for a task.
     *
     * @param taskId the ID of the task
     * @param decision the approval decision (e.g., "approve", "reject")
     * @param comments optional comments
     * @param userId the ID of the user recording the approval
     * @return the approval result DTO
     */
    @Transactional
    public ApprovalDTO recordApproval(String taskId, String decision, String comments, String userId) {
        log.debug("Recording approval for task {} by user {}. Decision: {}", taskId, userId, decision);
        Task task = getTaskOrThrow(taskId);
        String processInstanceId = task.getProcessInstanceId();
        String currentLevel = getCurrentLevel(processInstanceId);

        var result = historyRecorder.recordApprovalHistory(processInstanceId, taskId, task.getName(),
            userId, currentLevel, decision, comments, runtimeService.getVariables(processInstanceId));

        // Update variables
        Map<String, Object> updateVars = new HashMap<>();
        updateVars.put(WorkflowConstants.VAR_DECISION, decision.toLowerCase());
        updateVars.put(WorkflowConstants.VAR_APPROVAL_COMMENTS, comments);
        updateVars.put(WorkflowConstants.VAR_COMPLETED_BY, userId);

        taskService.complete(taskId, updateVars);

        log.info("Approval recorded for task {} (ProcessInstance: {}) by {} - Decision: {}", taskId, processInstanceId, userId, decision);

        return ApprovalDTO.builder()
                .id(result.id())
                .processInstanceId(processInstanceId)
                .taskId(taskId)
                .taskName(task.getName())
                .approverId(userId)
                .approverLevel(currentLevel)
                .decision(decision)
                .comments(comments)
                .timestamp(LocalDateTime.now())
                .stepOrder(result.stepOrder())
                .isRequired(true)
                .build();
    }
}
