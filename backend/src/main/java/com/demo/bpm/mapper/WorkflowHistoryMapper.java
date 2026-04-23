package com.demo.bpm.mapper;

import com.demo.bpm.dto.*;
import com.demo.bpm.util.WorkflowConstants;
import com.demo.bpm.util.WorkflowVariableUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class WorkflowHistoryMapper {

    private final ObjectMapper objectMapper;

    public WorkflowHistoryMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public TaskHistoryDTO toTaskHistoryDTO(HistoricTaskInstance task, List<HistoricVariableInstance> taskVars) {
        Map<String, Object> variables = new HashMap<>();
        for (HistoricVariableInstance var : taskVars) {
            variables.put(var.getVariableName(), var.getValue());
        }

        return TaskHistoryDTO.builder()
                .id(task.getId())
                .taskDefinitionKey(task.getTaskDefinitionKey())
                .name(task.getName())
                .description(task.getDescription())
                .processInstanceId(task.getProcessInstanceId())
                .assignee(task.getAssignee())
                .owner(task.getOwner())
                .createTime(task.getCreateTime().toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime())
                .claimTime(task.getClaimTime() != null ?
                        task.getClaimTime().toInstant()
                                .atZone(ZoneId.systemDefault())
                                .toLocalDateTime() : null)
                .endTime(task.getEndTime() != null ?
                        task.getEndTime().toInstant()
                                .atZone(ZoneId.systemDefault())
                                .toLocalDateTime() : null)
                .durationInMillis(task.getDurationInMillis())
                .deleteReason(task.getDeleteReason())
                .variables(variables)
                .build();
    }

    public List<EscalationDTO> toEscalationHistory(Map<String, Object> variables) {
        return WorkflowVariableUtils.getListVariable(variables, WorkflowConstants.VAR_ESCALATION_HISTORY, objectMapper).stream()
                .map(map -> EscalationDTO.builder()
                        .id((String) map.get("id"))
                        .taskId((String) map.get("taskId"))
                        .fromLevel((String) map.get("fromLevel"))
                        .toLevel((String) map.get("toLevel"))
                        .fromUserId((String) map.get("fromUserId"))
                        .reason((String) map.get("reason"))
                        .type((String) map.get("type"))
                        .timestamp(LocalDateTime.parse((String) map.get("timestamp")))
                        .build())
                .collect(Collectors.toList());
    }

    public List<ApprovalDTO> toApprovalHistory(String processInstanceId, Map<String, Object> variables) {
        return WorkflowVariableUtils.getListVariable(variables, WorkflowConstants.VAR_APPROVAL_HISTORY, objectMapper).stream()
                .map(map -> ApprovalDTO.builder()
                        .id((String) map.get("id"))
                        .processInstanceId(processInstanceId)
                        .taskId((String) map.get("taskId"))
                        .taskName((String) map.get("taskName"))
                        .approverId((String) map.get("approverId"))
                        .approverLevel((String) map.get("approverLevel"))
                        .decision((String) map.get("decision"))
                        .comments((String) map.get("comments"))
                        .timestamp(LocalDateTime.parse((String) map.get("timestamp")))
                        .stepOrder(((Number) map.getOrDefault("stepOrder", 0)).intValue())
                        .isRequired(true)
                        .build())
                .collect(Collectors.toList());
    }

    public List<CommentDTO> toCommentDTOs(List<org.flowable.engine.task.Comment> comments) {
        return comments.stream()
                .map(comment -> CommentDTO.builder()
                        .id(comment.getId())
                        .message(comment.getFullMessage())
                        .authorId(comment.getUserId())
                        .timestamp(comment.getTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime())
                        .build())
                .sorted(Comparator.comparing(CommentDTO::getTimestamp))
                .collect(Collectors.toList());
    }
}
