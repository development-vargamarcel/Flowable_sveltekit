package com.demo.bpm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowHistoryDTO {
    private String processInstanceId;
    private String processDefinitionKey;
    private String processDefinitionName;
    private String businessKey;
    private String status; // ACTIVE, COMPLETED, SUSPENDED, TERMINATED
    private String initiatorId;
    private String initiatorName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long durationInMillis;
    private String currentTaskId;
    private String currentTaskName;
    private String currentAssignee;
    private String currentLevel; // EMPLOYEE, SUPERVISOR, MANAGER, DIRECTOR, EXECUTIVE
    private Integer escalationCount;
    private Map<String, Object> variables;
    private List<TaskHistoryDTO> taskHistory;
    private List<EscalationDTO> escalationHistory;
    private List<ApprovalDTO> approvals;
    private List<CommentDTO> comments;
}
