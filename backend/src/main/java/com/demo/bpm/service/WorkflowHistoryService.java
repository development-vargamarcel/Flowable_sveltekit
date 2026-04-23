package com.demo.bpm.service;

import com.demo.bpm.dto.*;
import com.demo.bpm.exception.ResourceNotFoundException;
import com.demo.bpm.util.WorkflowConstants;
import com.demo.bpm.util.WorkflowVariableUtils;
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
    private final com.demo.bpm.mapper.WorkflowHistoryMapper workflowHistoryMapper;
    private final com.demo.bpm.service.helpers.VariableHelper variableHelper;

    public WorkflowHistoryDTO getWorkflowHistory(String processInstanceId) {
        ProcessInstance activeInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();

        WorkflowHistoryDTO.WorkflowHistoryDTOBuilder builder = WorkflowHistoryDTO.builder()
                .processInstanceId(processInstanceId);

        Map<String, Object> variables;
        if (activeInstance != null) {
            variables = mapActiveInstance(activeInstance, builder);
        } else {
            variables = mapHistoricInstance(processInstanceId, builder);
        }

        populateTaskInfo(processInstanceId, builder);
        populateHistorySegments(processInstanceId, variables, builder);

        return builder.build();
    }

    private Map<String, Object> mapActiveInstance(ProcessInstance instance, WorkflowHistoryDTO.WorkflowHistoryDTOBuilder builder) {
        String processInstanceId = instance.getId();
        Map<String, Object> variables = variableHelper.getMergedVariables(processInstanceId);

        ProcessDefinition definition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(instance.getProcessDefinitionId())
                .singleResult();

        builder.status(instance.isSuspended() ? "SUSPENDED" : "ACTIVE")
                .startTime(instance.getStartTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime())
                .processDefinitionId(instance.getProcessDefinitionId())
                .processDefinitionKey(instance.getProcessDefinitionKey())
                .processDefinitionName(definition != null ? definition.getName() : null)
                .businessKey(instance.getBusinessKey())
                .initiatorId((String) variables.get(WorkflowConstants.VAR_STARTED_BY))
                .initiatorName((String) variables.get(WorkflowConstants.VAR_EMPLOYEE_NAME))
                .currentLevel(WorkflowVariableUtils.getStringVariable(variables, WorkflowConstants.VAR_CURRENT_LEVEL, WorkflowConstants.LEVEL_SUPERVISOR))
                .escalationCount(WorkflowVariableUtils.getIntVariable(variables, WorkflowConstants.VAR_ESCALATION_COUNT, 0))
                .variables(variables);

        return variables;
    }

    private Map<String, Object> mapHistoricInstance(String processInstanceId, WorkflowHistoryDTO.WorkflowHistoryDTOBuilder builder) {
        HistoricProcessInstance historicInstance = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();

        if (historicInstance == null) {
            throw new ResourceNotFoundException("Process instance not found: " + processInstanceId);
        }

        Map<String, Object> variables = variableHelper.getMergedVariables(processInstanceId);

        builder.status(historicInstance.getEndTime() != null ? "COMPLETED" : "ACTIVE")
                .startTime(historicInstance.getStartTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime())
                .processDefinitionId(historicInstance.getProcessDefinitionId())
                .processDefinitionKey(historicInstance.getProcessDefinitionKey())
                .processDefinitionName(historicInstance.getProcessDefinitionName())
                .businessKey(historicInstance.getBusinessKey())
                .initiatorId(historicInstance.getStartUserId())
                .initiatorName((String) variables.get(WorkflowConstants.VAR_EMPLOYEE_NAME))
                .currentLevel(WorkflowVariableUtils.getStringVariable(variables, WorkflowConstants.VAR_CURRENT_LEVEL, WorkflowConstants.LEVEL_SUPERVISOR))
                .escalationCount(WorkflowVariableUtils.getIntVariable(variables, WorkflowConstants.VAR_ESCALATION_COUNT, 0))
                .variables(variables);

        if (historicInstance.getEndTime() != null) {
            builder.endTime(historicInstance.getEndTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime())
                    .durationInMillis(historicInstance.getDurationInMillis());
        }

        return variables;
    }

    private void populateTaskInfo(String processInstanceId, WorkflowHistoryDTO.WorkflowHistoryDTOBuilder builder) {
        List<Task> currentTasks = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .list();

        if (!currentTasks.isEmpty()) {
            Task currentTask = currentTasks.get(0);
            builder.currentTaskId(currentTask.getId())
                    .currentTaskName(currentTask.getName())
                    .currentAssignee(currentTask.getAssignee());
        }
    }

    private void populateHistorySegments(String processInstanceId, Map<String, Object> variables, WorkflowHistoryDTO.WorkflowHistoryDTOBuilder builder) {
        builder.taskHistory(getTaskHistory(processInstanceId))
                .escalationHistory(workflowHistoryMapper.toEscalationHistory(variables))
                .approvals(workflowHistoryMapper.toApprovalHistory(processInstanceId, variables))
                .comments(getComments(processInstanceId));
    }

    private List<CommentDTO> getComments(String processInstanceId) {
        log.debug("Fetching comments for process {}", processInstanceId);
        List<org.flowable.engine.task.Comment> comments = taskService.getProcessInstanceComments(processInstanceId);
        return workflowHistoryMapper.toCommentDTOs(comments);
    }

    public List<TaskHistoryDTO> getTaskHistory(String processInstanceId) {
        return historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(processInstanceId)
                .orderByHistoricTaskInstanceEndTime().asc()
                .list()
                .stream()
                .map(task -> {
                    List<HistoricVariableInstance> taskVars = historyService.createHistoricVariableInstanceQuery()
                            .taskId(task.getId())
                            .list();
                    return workflowHistoryMapper.toTaskHistoryDTO(task, taskVars);
                })
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

}
