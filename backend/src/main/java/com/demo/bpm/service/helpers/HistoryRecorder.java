package com.demo.bpm.service.helpers;

import com.demo.bpm.util.WorkflowConstants;
import com.demo.bpm.util.WorkflowVariableUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.RuntimeService;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class HistoryRecorder {

    private final ObjectMapper objectMapper;
    private final RuntimeService runtimeService;

    public String recordEscalationHistory(String processInstanceId, String taskId, String userId,
                                          String currentLevel, String targetLevel, String reason,
                                          boolean isEscalation, Map<String, Object> variables) {

        List<Map<String, Object>> history = WorkflowVariableUtils.getListVariable(
            variables, WorkflowConstants.VAR_ESCALATION_HISTORY, objectMapper);

        String type = isEscalation ? WorkflowConstants.TYPE_ESCALATE : WorkflowConstants.TYPE_DE_ESCALATE;
        String now = LocalDateTime.now().toString();
        String id = UUID.randomUUID().toString();

        Map<String, Object> record = new HashMap<>();
        record.put("id", id);
        record.put("taskId", taskId);
        record.put("fromLevel", currentLevel);
        record.put("toLevel", targetLevel);
        record.put("fromUserId", userId);
        record.put("reason", reason);
        record.put("type", type);
        record.put("timestamp", now);
        history.add(record);

        runtimeService.setVariable(processInstanceId, WorkflowConstants.VAR_ESCALATION_HISTORY,
            WorkflowVariableUtils.serializeList(history, objectMapper));

        return id;
    }

    public void recordHandoffHistory(String processInstanceId, String taskId, String taskName,
                                     String fromUserId, String toUserId, String reason,
                                     Map<String, Object> variables) {

        List<Map<String, Object>> history = WorkflowVariableUtils.getListVariable(
            variables, WorkflowConstants.VAR_HANDOFF_HISTORY, objectMapper);

        Map<String, Object> record = new HashMap<>();
        record.put("id", UUID.randomUUID().toString());
        record.put("taskId", taskId);
        record.put("taskName", taskName);
        record.put("fromUserId", fromUserId);
        record.put("toUserId", toUserId);
        record.put("reason", reason);
        record.put("timestamp", LocalDateTime.now().toString());
        history.add(record);

        runtimeService.setVariable(processInstanceId, WorkflowConstants.VAR_HANDOFF_HISTORY,
            WorkflowVariableUtils.serializeList(history, objectMapper));
    }

    public ApprovalRecordResult recordApprovalHistory(String processInstanceId, String taskId, String taskName,
                                      String userId, String currentLevel, String decision, String comments,
                                      Map<String, Object> variables) {

        List<Map<String, Object>> history = WorkflowVariableUtils.getListVariable(
            variables, WorkflowConstants.VAR_APPROVAL_HISTORY, objectMapper);

        String id = UUID.randomUUID().toString();
        int stepOrder = history.size() + 1;

        Map<String, Object> record = new HashMap<>();
        record.put("id", id);
        record.put("taskId", taskId);
        record.put("taskName", taskName);
        record.put("approverId", userId);
        record.put("approverLevel", currentLevel);
        record.put("decision", decision);
        record.put("comments", comments);
        record.put("timestamp", LocalDateTime.now().toString());
        record.put("stepOrder", stepOrder);
        history.add(record);

        runtimeService.setVariable(processInstanceId, WorkflowConstants.VAR_APPROVAL_HISTORY,
            WorkflowVariableUtils.serializeList(history, objectMapper));

        return new ApprovalRecordResult(id, stepOrder);
    }

    public record ApprovalRecordResult(String id, int stepOrder) {}
}
