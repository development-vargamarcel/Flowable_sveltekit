package com.demo.bpm.service;

import com.demo.bpm.dto.DocumentDTO;
import com.demo.bpm.dto.TaskDTO;
import com.demo.bpm.entity.ProcessConfig;
import com.demo.bpm.repository.ProcessConfigRepository;
import com.demo.bpm.util.VariableStorageUtil;
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
import java.util.Optional;
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
    private final ProcessConfigRepository processConfigRepository;

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

    /**
     * Get all variables for a task, merging:
     * 1. System variables from Flowable (variables starting with _)
     * 2. Business data from document/grid_rows tables
     */
    public Map<String, Object> getTaskVariables(String taskId) {
        Task task = flowableTaskService.createTaskQuery()
                .taskId(taskId)
                .singleResult();

        if (task == null) {
            throw new RuntimeException("Task not found: " + taskId);
        }

        return getMergedVariables(task.getProcessInstanceId());
    }

    /**
     * Get merged variables from both Flowable (system vars) and document tables (business data).
     */
    private Map<String, Object> getMergedVariables(String processInstanceId) {
        Map<String, Object> mergedVars = new HashMap<>();

        // Get system variables from Flowable (variables starting with _)
        try {
            Map<String, Object> flowableVars = runtimeService.getVariables(processInstanceId);
            if (flowableVars != null) {
                mergedVars.putAll(flowableVars);
            }
        } catch (Exception e) {
            log.debug("Could not get Flowable variables, process may have ended: {}", e.getMessage());
        }

        // Get business data from document table
        try {
            Optional<DocumentDTO> documentOpt = businessTableService.getDocument(processInstanceId, "main");
            if (documentOpt.isPresent()) {
                DocumentDTO document = documentOpt.get();

                // Add document fields
                if (document.getFields() != null) {
                    mergedVars.putAll(document.getFields());
                }

                // Add grid data
                if (document.getGrids() != null) {
                    mergedVars.putAll(document.getGrids());
                }
            }
        } catch (Exception e) {
            log.debug("Could not get document data: {}", e.getMessage());
        }

        return mergedVars;
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

        // Collect all variables (both system and business)
        Map<String, Object> allVars = new HashMap<>(variables != null ? variables : Map.of());

        // Add completion metadata with _ prefix so they're stored in Flowable
        allVars.put("_completedBy", userId);
        allVars.put("_completedAt", java.time.LocalDateTime.now().toString());

        // Only pass system variables (starting with _) to Flowable
        // Business variables will be stored in document/grid_rows tables only
        Map<String, Object> systemVars = VariableStorageUtil.filterSystemVariables(allVars);

        flowableTaskService.complete(taskId, systemVars);
        log.info("Task {} completed by {}. System vars: {}, Total vars: {}", taskId, userId, systemVars.size(), allVars.size());

        // Persist to business tables if configured
        if (processDefKey != null && businessTableService.shouldPersistOnTaskComplete(processDefKey)) {
            try {
                Optional<ProcessConfig> config = processConfigRepository.findByProcessDefinitionKey(processDefKey);
                String documentType = config.map(ProcessConfig::getDocumentType).orElse(null);

                // Save all submitted variables to business tables
                // Note: System variables (starting with _) are stored in Flowable
                // Business variables are stored only in document/grid_rows
                // We pass all variables here so system metadata is also available in document if needed
                businessTableService.saveAllData(
                        processInstanceId,
                        businessKey,
                        processDefKey,
                        processDefName,
                        documentType,
                        allVars,
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
        // Get merged variables from both Flowable (system vars) and document tables (business data)
        Map<String, Object> variables = getMergedVariables(task.getProcessInstanceId());

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
