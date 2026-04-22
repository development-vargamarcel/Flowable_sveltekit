package com.demo.bpm.service;

import com.demo.bpm.dto.DocumentDTO;
import com.demo.bpm.dto.TaskDTO;
import com.demo.bpm.entity.ProcessConfig;
import com.demo.bpm.exception.InvalidOperationException;
import com.demo.bpm.exception.ResourceNotFoundException;
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
    private final com.demo.bpm.service.helpers.TaskCommonHelper taskCommonHelper;
    private final com.demo.bpm.service.helpers.VariableHelper variableHelper;

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
        return getGroupTasks(userId, null, null, null);
    }

    /**
     * Retrieves tasks assigned to the user or their groups with filtering.
     *
     * @param userId the ID of the user
     * @param text filter by task name (partial)
     * @param assignee filter by assignee
     * @param priority filter by priority
     * @return list of tasks
     */
    public List<TaskDTO> getGroupTasks(String userId, String text, String assignee, Integer priority) {
        log.debug("Fetching group tasks for user: {}, text: {}, assignee: {}, priority: {}", userId, text, assignee, priority);
        org.flowable.task.api.TaskQuery query = flowableTaskService.createTaskQuery()
                .taskCandidateOrAssigned(userId);

        if (text != null && !text.isBlank()) {
            query.taskNameLikeIgnoreCase("%" + text.trim() + "%");
        }

        if (priority != null) {
            query.taskPriority(priority);
        }

        if (assignee != null && !assignee.isBlank()) {
             if ("unassigned".equalsIgnoreCase(assignee)) {
                 query.taskUnassigned();
             } else {
                 query.taskAssignee(assignee);
             }
        }

        return getTasks(query);
    }

    public TaskDTO getTaskById(String taskId) {
        return convertToDTO(taskCommonHelper.getTaskOrThrow(taskId));
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
        Task task = taskCommonHelper.getTaskOrThrow(taskId);
        return variableHelper.getMergedVariables(task.getProcessInstanceId());
    }

    public String getProcessDefinitionIdForTask(String taskId) {
        return taskCommonHelper.getTaskOrThrow(taskId).getProcessDefinitionId();
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

        Task task = taskCommonHelper.getTaskOrThrow(taskId);

        // Validate access - only assignee can delegate
        taskCommonHelper.validateIsAssignee(task, currentUserId);

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
        Task task = taskCommonHelper.getTaskOrThrow(taskId);

        if (task.getAssignee() != null) {
            throw new InvalidOperationException("Task is already assigned to: " + task.getAssignee());
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
        taskCommonHelper.getTaskOrThrow(taskId);
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
        Task task = taskCommonHelper.getTaskOrThrow(taskId);

        // Allow completion if user is assignee or task is unassigned
        taskCommonHelper.validateAssigneeOrUnassigned(task, userId);

        internalCompleteTask(task, variables, userId);
    }

    /**
     * Internal method for task completion shared by various workflow actions.
     * Assumes validation has already been performed.
     *
     * @param task the task to complete
     * @param variables the variables to complete the task with
     * @param userId the user performing the completion
     */
    protected void internalCompleteTask(Task task, Map<String, Object> variables, String userId) {
        String taskId = task.getId();

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
        if (!allVars.containsKey("_completedBy")) {
            allVars.put("_completedBy", userId);
        }
        if (!allVars.containsKey("_completedAt")) {
            allVars.put("_completedAt", java.time.LocalDateTime.now().toString());
        }

        // Only pass system variables (starting with _) to Flowable
        Map<String, Object> systemVars = VariableStorageUtil.filterSystemVariables(allVars);

        flowableTaskService.complete(taskId, systemVars);
        log.info("Task {} completed by {}. System vars: {}, Total vars: {}", taskId, userId, systemVars.size(), allVars.size());

        // Persist to business tables if configured
        if (processDefKey != null && businessTableService.shouldPersistOnTaskComplete(processDefKey)) {
            try {
                Optional<ProcessConfig> config = processConfigRepository.findByProcessDefinitionKey(processDefKey);
                String documentType = config.map(ProcessConfig::getDocumentType).orElse(null);

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
            }
        }
    }

    private TaskDTO convertToDTO(Task task) {
        // Get merged variables from both Flowable (system vars) and document tables (business data)
        Map<String, Object> variables = variableHelper.getMergedVariables(task.getProcessInstanceId());
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
