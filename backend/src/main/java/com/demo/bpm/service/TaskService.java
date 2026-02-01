package com.demo.bpm.service;

import com.demo.bpm.dto.DocumentDTO;
import com.demo.bpm.dto.TaskDTO;
import com.demo.bpm.entity.ProcessConfig;
import com.demo.bpm.repository.ProcessConfigRepository;
import com.demo.bpm.service.helpers.TaskQueryHelper;
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
    private final TaskQueryHelper taskQueryHelper;

    /**
     * Retrieves tasks assigned to the specific user.
     *
     * @param userId the ID of the user
     * @return list of assigned tasks
     */
    public List<TaskDTO> getAssignedTasks(String userId) {
        log.debug("Fetching assigned tasks for user: {}", userId);
        return getTasks(flowableTaskService.createTaskQuery().taskAssignee(userId));
    }

    /**
     * Retrieves tasks that the user can claim (candidate tasks).
     *
     * @param userId the ID of the user
     * @return list of claimable tasks
     */
    public List<TaskDTO> getClaimableTasks(String userId) {
        log.debug("Fetching claimable tasks for user: {}", userId);
        return getTasks(flowableTaskService.createTaskQuery().taskCandidateUser(userId));
    }

    /**
     * Retrieves tasks assigned to the user or their groups (candidate or assigned).
     *
     * @param userId the ID of the user
     * @return list of tasks
     */
    public List<TaskDTO> getGroupTasks(String userId) {
        log.debug("Fetching group tasks for user: {}", userId);
        return getTasks(flowableTaskService.createTaskQuery().taskCandidateOrAssigned(userId));
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

    public Map<String, Object> getTaskDetails(String taskId) {
        TaskDTO task = getTaskById(taskId);
        Map<String, Object> variables = getTaskVariables(taskId);
        Map<String, Object> taskDetails = new HashMap<>();
        taskDetails.put("task", task);
        taskDetails.put("variables", variables);
        return taskDetails;
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

    /**
     * Delegates (reassigns) a task to another user.
     *
     * @param taskId the ID of the task
     * @param currentUserId the ID of the user currently assigned (or admin)
     * @param targetUserId the ID of the user to delegate to
     */
    @Transactional
    public void delegateTask(String taskId, String currentUserId, String targetUserId) {
        log.debug("User {} delegating task {} to {}", currentUserId, taskId, targetUserId);

        Task task = flowableTaskService.createTaskQuery()
                .taskId(taskId)
                .singleResult();

        if (task == null) {
            throw new RuntimeException("Task not found: " + taskId);
        }

        // Validate access - only assignee can delegate (or we could add admin check)
        if (task.getAssignee() != null && !task.getAssignee().equals(currentUserId)) {
            // For now, strict check: only assignee can delegate
            throw new RuntimeException("Only the assignee can delegate the task");
        }

        // If task is unassigned, maybe allow claiming implicitly?
        // But usually delegation implies "I have it, I give it to you".
        if (task.getAssignee() == null) {
             // For unassigned tasks, users should use "claim" first, or we allow "assign" if we want.
             // Let's enforce claim first to avoid confusion, or just allow it.
             // If I am not assignee (and it's null), I can't delegate based on check above.
             // So this check covers it.
        }

        flowableTaskService.setAssignee(taskId, targetUserId);
        log.info("Task {} delegated/reassigned from {} to {}", taskId, currentUserId, targetUserId);
    }

    /**
     * Claims a task for a user.
     *
     * @param taskId the ID of the task
     * @param userId the ID of the user claiming the task
     */
    @Transactional
    public void claimTask(String taskId, String userId) {
        log.debug("User {} attempting to claim task {}", userId, taskId);
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
        log.info("Task {} successfully claimed by {}", taskId, userId);
    }

    /**
     * Unclaims a task (sets assignee to null).
     *
     * @param taskId the ID of the task
     */
    @Transactional
    public void unclaimTask(String taskId) {
        log.debug("Unclaiming task {}", taskId);
        Task task = flowableTaskService.createTaskQuery()
                .taskId(taskId)
                .singleResult();

        if (task == null) {
            throw new RuntimeException("Task not found: " + taskId);
        }

        flowableTaskService.unclaim(taskId);
        log.info("Task {} unclaimed (assignee removed)", taskId);
    }

    /**
     * Completes a task with the given variables.
     *
     * @param taskId the ID of the task
     * @param variables the variables to complete the task with
     * @param userId the ID of the user completing the task
     */
    @Transactional
    public void completeTask(String taskId, Map<String, Object> variables, String userId) {
        log.debug("User {} completing task {} with variables: {}", userId, taskId, variables);
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
        return taskQueryHelper.convertToDTO(task, variables);
    }

    private List<TaskDTO> getTasks(org.flowable.task.api.TaskQuery query) {
        return query.orderByTaskPriority().desc()
                .orderByTaskCreateTime().desc()
                .list()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
}
