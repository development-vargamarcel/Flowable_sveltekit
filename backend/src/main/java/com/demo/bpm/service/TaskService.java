package com.demo.bpm.service;

import com.demo.bpm.dto.TaskDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final org.flowable.engine.TaskService flowableTaskService;
    private final RuntimeService runtimeService;
    private final RepositoryService repositoryService;
    private final HistoryService historyService;
    private final BusinessTableService businessTableService;

    public List<TaskDTO> getAssignedTasks(String userId) {
        List<Task> tasks = flowableTaskService.createTaskQuery()
                .taskAssignee(userId)
                .orderByTaskPriority().desc()
                .orderByTaskCreateTime().desc()
                .list();

        return tasks.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<TaskDTO> getClaimableTasks(String userId) {
        List<Task> tasks = flowableTaskService.createTaskQuery()
                .taskCandidateUser(userId)
                .orderByTaskPriority().desc()
                .orderByTaskCreateTime().desc()
                .list();

        return tasks.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<TaskDTO> getGroupTasks(String userId) {
        List<Task> tasks = flowableTaskService.createTaskQuery()
                .taskCandidateOrAssigned(userId)
                .orderByTaskPriority().desc()
                .orderByTaskCreateTime().desc()
                .list();

        return tasks.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public TaskDTO getTaskById(String taskId) {
        Task task = flowableTaskService.createTaskQuery()
                .taskId(taskId)
                .singleResult();

        if (task == null) {
            throw new RuntimeException("Task not found: " + taskId);
        }

        return convertToDTO(task);
    }

    public Map<String, Object> getTaskVariables(String taskId) {
        return flowableTaskService.getVariables(taskId);
    }

    public String getProcessDefinitionIdForTask(String taskId) {
        Task task = flowableTaskService.createTaskQuery()
                .taskId(taskId)
                .singleResult();

        if (task == null) {
            throw new RuntimeException("Task not found: " + taskId);
        }

        return task.getProcessDefinitionId();
    }

    @Transactional
    public void claimTask(String taskId, String userId) {
        Task task = flowableTaskService.createTaskQuery()
                .taskId(taskId)
                .singleResult();

        if (task == null) {
            throw new RuntimeException("Task not found: " + taskId);
        }

        if (task.getAssignee() != null) {
            throw new RuntimeException("Task is already assigned to: " + task.getAssignee());
        }

        flowableTaskService.claim(taskId, userId);
        log.info("Task {} claimed by {}", taskId, userId);
    }

    @Transactional
    public void completeTask(String taskId, Map<String, Object> variables, String userId) {
        Task task = flowableTaskService.createTaskQuery()
                .taskId(taskId)
                .singleResult();

        if (task == null) {
            throw new RuntimeException("Task not found: " + taskId);
        }

        // Allow completion if user is assignee or task is unassigned
        if (task.getAssignee() != null && !task.getAssignee().equals(userId)) {
            throw new RuntimeException("Task is assigned to another user");
        }

        // If task is unassigned, claim it first
        if (task.getAssignee() == null) {
            flowableTaskService.claim(taskId, userId);
        }

        // Get process info for business table persistence
        String processInstanceId = task.getProcessInstanceId();
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();

        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(task.getProcessDefinitionId())
                .singleResult();

        String processDefKey = processDefinition != null ? processDefinition.getKey() : null;
        String processDefName = processDefinition != null ? processDefinition.getName() : null;
        String businessKey = processInstance != null ? processInstance.getBusinessKey() : null;

        // Add completion metadata
        Map<String, Object> completeVars = new HashMap<>(variables != null ? variables : Map.of());
        completeVars.put("completedBy", userId);
        completeVars.put("completedAt", java.time.LocalDateTime.now().toString());

        flowableTaskService.complete(taskId, completeVars);
        log.info("Task {} completed by {} with variables: {}", taskId, userId, variables);

        // Persist to business tables if configured
        if (processDefKey != null && businessTableService.shouldPersistOnTaskComplete(processDefKey)) {
            try {
                // Merge existing process variables with new ones for complete picture
                Map<String, Object> allVariables = new HashMap<>();

                // Get all process variables (includes previously set values)
                try {
                    Map<String, Object> processVars = runtimeService.getVariables(processInstanceId);
                    allVariables.putAll(processVars);
                } catch (Exception e) {
                    // Process might have ended, that's ok
                    log.debug("Could not get process variables, process may have ended: {}", e.getMessage());
                }

                // Overlay with submitted variables (latest values take precedence)
                if (variables != null) {
                    allVariables.putAll(variables);
                }

                businessTableService.saveAllData(
                        processInstanceId,
                        businessKey,
                        processDefKey,
                        processDefName,
                        allVariables,
                        userId
                );
                log.info("Business table data saved for process instance: {}", processInstanceId);
            } catch (Exception e) {
                log.error("Failed to save business table data for process {}: {}", processInstanceId, e.getMessage(), e);
                // Don't fail the task completion if business table save fails
            }
        }
    }

    private TaskDTO convertToDTO(Task task) {
        Map<String, Object> variables = flowableTaskService.getVariables(task.getId());

        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(task.getProcessInstanceId())
                .singleResult();

        String businessKey = processInstance != null ? processInstance.getBusinessKey() : null;

        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(task.getProcessDefinitionId())
                .singleResult();

        String processName = processDefinition != null ? processDefinition.getName() : task.getProcessDefinitionId();
        String processKey = processDefinition != null ? processDefinition.getKey() : null;

        return TaskDTO.builder()
                .id(task.getId())
                .name(task.getName())
                .description(task.getDescription())
                .processInstanceId(task.getProcessInstanceId())
                .processDefinitionKey(processKey)
                .processName(processName)
                .assignee(task.getAssignee())
                .owner(task.getOwner())
                .createTime(task.getCreateTime().toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime())
                .dueDate(task.getDueDate() != null
                        ? task.getDueDate().toInstant()
                                .atZone(ZoneId.systemDefault())
                                .toLocalDateTime()
                        : null)
                .priority(task.getPriority())
                .formKey(task.getFormKey())
                .variables(variables)
                .businessKey(businessKey)
                .build();
    }
}
