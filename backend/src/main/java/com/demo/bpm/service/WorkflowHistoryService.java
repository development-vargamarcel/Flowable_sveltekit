package com.demo.bpm.service;

import com.demo.bpm.dto.*;
import com.demo.bpm.exception.ResourceNotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowHistoryService {

    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final HistoryService historyService;
    private final RepositoryService repositoryService;
    private final ObjectMapper objectMapper;

    public WorkflowHistoryDTO getWorkflowHistory(String processInstanceId) {
        // First try active process
        ProcessInstance activeInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();

        Map<String, Object> variables;
        String status;
        LocalDateTime startTime;
        LocalDateTime endTime = null;
        String processDefKey;
        String processDefName;
        String businessKey;
        String startUserId;
        Long durationInMillis = null;

        if (activeInstance != null) {
            variables = runtimeService.getVariables(processInstanceId);
            status = "ACTIVE";
            startTime = activeInstance.getStartTime().toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
            processDefKey = activeInstance.getProcessDefinitionKey();
            businessKey = activeInstance.getBusinessKey();

            ProcessDefinition definition = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionId(activeInstance.getProcessDefinitionId())
                    .singleResult();
            processDefName = definition != null ? definition.getName() : null;
            startUserId = (String) variables.get("startedBy");
        } else {
            // Check history
            HistoricProcessInstance historicInstance = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();

            if (historicInstance == null) {
                throw new ResourceNotFoundException("Process instance not found: " + processInstanceId);
            }

            List<HistoricVariableInstance> historicVars = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .list();

            variables = new HashMap<>();
            for (HistoricVariableInstance var : historicVars) {
                variables.put(var.getVariableName(), var.getValue());
            }

            status = historicInstance.getEndTime() != null ? "COMPLETED" : "ACTIVE";
            startTime = historicInstance.getStartTime().toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
            if (historicInstance.getEndTime() != null) {
                endTime = historicInstance.getEndTime().toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime();
                durationInMillis = historicInstance.getDurationInMillis();
            }
            processDefKey = historicInstance.getProcessDefinitionKey();
            processDefName = historicInstance.getProcessDefinitionName();
            businessKey = historicInstance.getBusinessKey();
            startUserId = historicInstance.getStartUserId();
        }

        // Get current task info
        String currentTaskId = null;
        String currentTaskName = null;
        String currentAssignee = null;
        List<Task> currentTasks = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .list();
        if (!currentTasks.isEmpty()) {
            Task currentTask = currentTasks.get(0);
            currentTaskId = currentTask.getId();
            currentTaskName = currentTask.getName();
            currentAssignee = currentTask.getAssignee();
        }

        // Get task history
        List<TaskHistoryDTO> taskHistory = getTaskHistory(processInstanceId);

        // Get escalation history
        List<EscalationDTO> escalationHistory = getEscalationHistory(variables);

        // Get approvals
        List<ApprovalDTO> approvals = getApprovalHistory(processInstanceId, variables);

        return WorkflowHistoryDTO.builder()
                .processInstanceId(processInstanceId)
                .processDefinitionKey(processDefKey)
                .processDefinitionName(processDefName)
                .businessKey(businessKey)
                .status(status)
                .initiatorId(startUserId)
                .initiatorName((String) variables.get("employeeName"))
                .startTime(startTime)
                .endTime(endTime)
                .durationInMillis(durationInMillis)
                .currentTaskId(currentTaskId)
                .currentTaskName(currentTaskName)
                .currentAssignee(currentAssignee)
                .currentLevel((String) variables.getOrDefault("currentLevel", "SUPERVISOR"))
                .escalationCount(((Number) variables.getOrDefault("escalationCount", 0)).intValue())
                .variables(variables)
                .taskHistory(taskHistory)
                .escalationHistory(escalationHistory)
                .approvals(approvals)
                .build();
    }

    public List<TaskHistoryDTO> getTaskHistory(String processInstanceId) {
        return historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(processInstanceId)
                .orderByHistoricTaskInstanceEndTime().asc()
                .list()
                .stream()
                .map(this::convertToTaskHistoryDTO)
                .collect(Collectors.toList());
    }

    public List<WorkflowHistoryDTO> getAllProcesses(String status, String processType, int page, int size) {
        List<WorkflowHistoryDTO> result = new ArrayList<>();

        if ("ACTIVE".equals(status) || status == null) {
            var query = runtimeService.createProcessInstanceQuery()
                    .orderByStartTime().desc();

            if (processType != null) {
                query.processDefinitionKey(processType);
            }

            List<ProcessInstance> activeList = query.listPage(page * size, size);

            for (ProcessInstance pi : activeList) {
                result.add(getWorkflowHistory(pi.getId()));
            }
        }

        if ("COMPLETED".equals(status) || status == null) {
            var query = historyService.createHistoricProcessInstanceQuery()
                    .finished()
                    .orderByProcessInstanceEndTime().desc();

            if (processType != null) {
                query.processDefinitionKey(processType);
            }

            List<HistoricProcessInstance> historicList = query.listPage(page * size, size);

            for (HistoricProcessInstance hpi : historicList) {
                result.add(getWorkflowHistory(hpi.getId()));
            }
        }

        return result;
    }

    // Helper methods
    private TaskHistoryDTO convertToTaskHistoryDTO(HistoricTaskInstance task) {
        Map<String, Object> variables = new HashMap<>();
        List<HistoricVariableInstance> taskVars = historyService.createHistoricVariableInstanceQuery()
                .taskId(task.getId())
                .list();
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

    private List<EscalationDTO> getEscalationHistory(Map<String, Object> variables) {
        return getEscalationHistoryList(variables).stream()
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

    private List<ApprovalDTO> getApprovalHistory(String processInstanceId, Map<String, Object> variables) {
        return getApprovalHistoryList(variables).stream()
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
}
