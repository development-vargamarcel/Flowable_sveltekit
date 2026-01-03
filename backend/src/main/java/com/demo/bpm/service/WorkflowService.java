package com.demo.bpm.service;

import com.demo.bpm.dto.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowService {

    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final HistoryService historyService;
    private final RepositoryService repositoryService;
    private final ObjectMapper objectMapper;

    private static final Map<String, String[]> ESCALATION_HIERARCHY = Map.of(
            "SUPERVISOR", new String[]{"MANAGER"},
            "MANAGER", new String[]{"DIRECTOR"},
            "DIRECTOR", new String[]{"EXECUTIVE"},
            "EXECUTIVE", new String[]{}
    );

    private static final Map<String, String[]> DE_ESCALATION_HIERARCHY = Map.of(
            "EXECUTIVE", new String[]{"DIRECTOR"},
            "DIRECTOR", new String[]{"MANAGER"},
            "MANAGER", new String[]{"SUPERVISOR"},
            "SUPERVISOR", new String[]{}
    );

    private static final Map<String, String> LEVEL_TO_GROUP = Map.of(
            "SUPERVISOR", "supervisors",
            "MANAGER", "managers",
            "DIRECTOR", "directors",
            "EXECUTIVE", "executives"
    );

    @Transactional
    public EscalationDTO escalateTask(String taskId, EscalationRequest request, String userId) {
        Task task = taskService.createTaskQuery()
                .taskId(taskId)
                .singleResult();

        if (task == null) {
            throw new RuntimeException("Task not found: " + taskId);
        }

        Map<String, Object> variables = runtimeService.getVariables(task.getProcessInstanceId());
        String currentLevel = (String) variables.getOrDefault("currentLevel", "SUPERVISOR");

        String[] nextLevels = ESCALATION_HIERARCHY.getOrDefault(currentLevel, new String[]{});
        if (nextLevels.length == 0) {
            throw new RuntimeException("Cannot escalate beyond " + currentLevel);
        }

        String targetLevel = request.getTargetLevel() != null ? request.getTargetLevel() : nextLevels[0];

        // Validate target level is in hierarchy
        if (!Arrays.asList(nextLevels).contains(targetLevel)) {
            throw new RuntimeException("Invalid escalation target: " + targetLevel);
        }

        // Record escalation in history
        int escalationCount = ((Number) variables.getOrDefault("escalationCount", 0)).intValue() + 1;
        List<Map<String, Object>> escalationHistory = getEscalationHistoryList(variables);

        Map<String, Object> escalationRecord = new HashMap<>();
        escalationRecord.put("id", UUID.randomUUID().toString());
        escalationRecord.put("taskId", taskId);
        escalationRecord.put("fromLevel", currentLevel);
        escalationRecord.put("toLevel", targetLevel);
        escalationRecord.put("fromUserId", userId);
        escalationRecord.put("reason", request.getReason());
        escalationRecord.put("type", "ESCALATE");
        escalationRecord.put("timestamp", LocalDateTime.now().toString());
        escalationHistory.add(escalationRecord);

        // Update process variables
        Map<String, Object> updateVars = new HashMap<>();
        updateVars.put("currentLevel", targetLevel);
        updateVars.put("escalationCount", escalationCount);
        updateVars.put("escalationHistory", serializeList(escalationHistory));
        updateVars.put("decision", "escalate");
        updateVars.put("escalationReason", request.getReason());
        updateVars.put("escalatedBy", userId);
        updateVars.put("escalatedAt", LocalDateTime.now().toString());

        // Complete current task with escalation decision
        taskService.complete(taskId, updateVars);

        log.info("Task {} escalated from {} to {} by {}", taskId, currentLevel, targetLevel, userId);

        return EscalationDTO.builder()
                .id((String) escalationRecord.get("id"))
                .taskId(taskId)
                .processInstanceId(task.getProcessInstanceId())
                .fromUserId(userId)
                .fromLevel(currentLevel)
                .toLevel(targetLevel)
                .reason(request.getReason())
                .type("ESCALATE")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Transactional
    public EscalationDTO deEscalateTask(String taskId, EscalationRequest request, String userId) {
        Task task = taskService.createTaskQuery()
                .taskId(taskId)
                .singleResult();

        if (task == null) {
            throw new RuntimeException("Task not found: " + taskId);
        }

        Map<String, Object> variables = runtimeService.getVariables(task.getProcessInstanceId());
        String currentLevel = (String) variables.getOrDefault("currentLevel", "SUPERVISOR");

        String[] prevLevels = DE_ESCALATION_HIERARCHY.getOrDefault(currentLevel, new String[]{});
        if (prevLevels.length == 0) {
            throw new RuntimeException("Cannot de-escalate below " + currentLevel);
        }

        String targetLevel = request.getTargetLevel() != null ? request.getTargetLevel() : prevLevels[0];

        // Record de-escalation in history
        List<Map<String, Object>> escalationHistory = getEscalationHistoryList(variables);

        Map<String, Object> deEscalationRecord = new HashMap<>();
        deEscalationRecord.put("id", UUID.randomUUID().toString());
        deEscalationRecord.put("taskId", taskId);
        deEscalationRecord.put("fromLevel", currentLevel);
        deEscalationRecord.put("toLevel", targetLevel);
        deEscalationRecord.put("fromUserId", userId);
        deEscalationRecord.put("reason", request.getReason());
        deEscalationRecord.put("type", "DE_ESCALATE");
        deEscalationRecord.put("timestamp", LocalDateTime.now().toString());
        escalationHistory.add(deEscalationRecord);

        // Update process variables
        Map<String, Object> updateVars = new HashMap<>();
        updateVars.put("currentLevel", targetLevel);
        updateVars.put("escalationHistory", serializeList(escalationHistory));
        updateVars.put("decision", "de_escalate");
        updateVars.put("deEscalationReason", request.getReason());
        updateVars.put("deEscalatedBy", userId);
        updateVars.put("deEscalatedAt", LocalDateTime.now().toString());

        // Complete current task with de-escalation decision
        taskService.complete(taskId, updateVars);

        log.info("Task {} de-escalated from {} to {} by {}", taskId, currentLevel, targetLevel, userId);

        return EscalationDTO.builder()
                .id((String) deEscalationRecord.get("id"))
                .taskId(taskId)
                .processInstanceId(task.getProcessInstanceId())
                .fromUserId(userId)
                .fromLevel(currentLevel)
                .toLevel(targetLevel)
                .reason(request.getReason())
                .type("DE_ESCALATE")
                .timestamp(LocalDateTime.now())
                .build();
    }

    public List<String> getEscalationOptions(String taskId) {
        Task task = taskService.createTaskQuery()
                .taskId(taskId)
                .singleResult();

        if (task == null) {
            throw new RuntimeException("Task not found: " + taskId);
        }

        Map<String, Object> variables = runtimeService.getVariables(task.getProcessInstanceId());
        String currentLevel = (String) variables.getOrDefault("currentLevel", "SUPERVISOR");

        return Arrays.asList(ESCALATION_HIERARCHY.getOrDefault(currentLevel, new String[]{}));
    }

    public List<String> getDeEscalationOptions(String taskId) {
        Task task = taskService.createTaskQuery()
                .taskId(taskId)
                .singleResult();

        if (task == null) {
            throw new RuntimeException("Task not found: " + taskId);
        }

        Map<String, Object> variables = runtimeService.getVariables(task.getProcessInstanceId());
        String currentLevel = (String) variables.getOrDefault("currentLevel", "SUPERVISOR");

        return Arrays.asList(DE_ESCALATION_HIERARCHY.getOrDefault(currentLevel, new String[]{}));
    }

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
                throw new RuntimeException("Process instance not found: " + processInstanceId);
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

    public DashboardDTO getDashboard(String userId, Pageable pageable, String status, String type) {
        // Get counts for stats
        long totalActive = runtimeService.createProcessInstanceQuery().count();
        long totalCompleted = historyService.createHistoricProcessInstanceQuery().finished().count();
        long totalPending = taskService.createTaskQuery().count();
        long myTasks = taskService.createTaskQuery().taskCandidateOrAssigned(userId).count();
        long myProcesses = runtimeService.createProcessInstanceQuery().variableValueEquals("startedBy", userId).count();
        long pendingEscalations = runtimeService.createProcessInstanceQuery().variableValueGreaterThan("escalationCount", 0).count();

        // Get paginated lists for display
        List<ProcessInstance> activeProcessesForDisplay = runtimeService.createProcessInstanceQuery()
                .orderByStartTime().desc()
                .listPage((int) pageable.getOffset(), pageable.getPageSize());

        List<HistoricProcessInstance> completedProcesses = historyService.createHistoricProcessInstanceQuery()
                .finished()
                .orderByProcessInstanceEndTime().desc()
                .listPage((int) pageable.getOffset(), pageable.getPageSize());

        // Calculate average completion time from a sample of recent completed processes
        long avgCompletionTimeHours = 0;
        if (!completedProcesses.isEmpty()) {
            long totalMillis = completedProcesses.stream()
                    .filter(p -> p.getDurationInMillis() != null)
                    .mapToLong(HistoricProcessInstance::getDurationInMillis)
                    .sum();
            if (completedProcesses.size() > 0) {
                avgCompletionTimeHours = totalMillis / (completedProcesses.size() * 3600000);
            }
        }

        // For activeByType, query a smaller sample for representative distribution
        // 100 is enough to get process type distribution without performance impact
        List<ProcessInstance> activeProcessesForGrouping = runtimeService.createProcessInstanceQuery()
                .orderByStartTime().desc()
                .listPage(0, 100);
        Map<String, Long> activeByType = activeProcessesForGrouping.stream()
                .collect(Collectors.groupingBy(ProcessInstance::getProcessDefinitionKey, Collectors.counting()));

        // Group by status
        Map<String, Long> byStatus = new HashMap<>();
        byStatus.put("ACTIVE", totalActive);
        byStatus.put("COMPLETED", totalCompleted);
        byStatus.put("PENDING", totalPending);

        // Get recent completed DTOs
        Page<WorkflowHistoryDTO> recentCompleted = new PageImpl<>(completedProcesses.stream()
                .map(hp -> getWorkflowHistory(hp.getId()))
                .collect(Collectors.toList()), pageable, totalCompleted);

        // Get active with details from the paginated list
        Page<WorkflowHistoryDTO> activeWithDetails = new PageImpl<>(activeProcessesForDisplay.stream()
                .map(ap -> getWorkflowHistory(ap.getId()))
                .collect(Collectors.toList()), pageable, totalActive);

        // Get user's pending approvals with pagination
        List<Task> userTasks = taskService.createTaskQuery()
                .taskCandidateOrAssigned(userId)
                .orderByTaskCreateTime().desc()
                .listPage((int) pageable.getOffset(), pageable.getPageSize());

        Page<WorkflowHistoryDTO> myPendingApprovals = new PageImpl<>(userTasks.stream()
                .map(t -> getWorkflowHistory(t.getProcessInstanceId()))
                .distinct()
                .collect(Collectors.toList()), pageable, myTasks);

        // Escalation metrics - use efficient count-based approach to avoid N+1 queries
        // Instead of iterating through all processes, we use the already-calculated counts
        // and sample a small subset for detailed escalation level breakdown
        long activeEscalatedProcesses = pendingEscalations;
        long totalEscalations = pendingEscalations; // Approximation based on escalated count
        long totalDeEscalations = 0;
        Map<String, Long> escalationsByLevel = new HashMap<>();

        // Only sample a small number of escalated processes for level breakdown
        // This avoids the N+1 query problem while still providing useful metrics
        List<ProcessInstance> escalatedSample = runtimeService.createProcessInstanceQuery()
                .variableValueGreaterThan("escalationCount", 0)
                .orderByStartTime().desc()
                .listPage(0, 50);

        for (ProcessInstance pi : escalatedSample) {
            try {
                Object levelObj = runtimeService.getVariable(pi.getId(), "currentLevel");
                if (levelObj != null) {
                    String currentLevel = levelObj.toString();
                    escalationsByLevel.merge(currentLevel, 1L, Long::sum);
                }
            } catch (Exception e) {
                log.debug("Could not get currentLevel for process {}: {}", pi.getId(), e.getMessage());
            }
        }

        DashboardDTO.DashboardStats stats = DashboardDTO.DashboardStats.builder()
                .totalActive(totalActive)
                .totalCompleted(totalCompleted)
                .totalPending(totalPending)
                .myTasks(myTasks)
                .myProcesses(myProcesses)
                .pendingEscalations(pendingEscalations)
                .avgCompletionTimeHours(avgCompletionTimeHours)
                .build();

        DashboardDTO.EscalationMetrics escalationMetrics = DashboardDTO.EscalationMetrics.builder()
                .totalEscalations(totalEscalations)
                .totalDeEscalations(totalDeEscalations)
                .activeEscalatedProcesses(activeEscalatedProcesses)
                .escalationsByLevel(escalationsByLevel)
                .build();

        return DashboardDTO.builder()
                .stats(stats)
                .activeByType(activeByType)
                .byStatus(byStatus)
                .recentCompleted(recentCompleted)
                .activeProcesses(activeWithDetails)
                .myPendingApprovals(myPendingApprovals)
                .escalationMetrics(escalationMetrics)
                .build();
    }

    public List<WorkflowHistoryDTO> getAllProcesses(String status, String processType, int page, int size) {
        List<WorkflowHistoryDTO> result = new ArrayList<>();

        if ("ACTIVE".equals(status) || status == null) {
            List<ProcessInstance> activeList = runtimeService.createProcessInstanceQuery()
                    .orderByStartTime().desc()
                    .listPage(page * size, size);

            for (ProcessInstance pi : activeList) {
                if (processType == null || processType.equals(pi.getProcessDefinitionKey())) {
                    result.add(getWorkflowHistory(pi.getId()));
                }
            }
        }

        if ("COMPLETED".equals(status) || status == null) {
            List<HistoricProcessInstance> historicList = historyService.createHistoricProcessInstanceQuery()
                    .finished()
                    .orderByProcessInstanceEndTime().desc()
                    .listPage(page * size, size);

            for (HistoricProcessInstance hpi : historicList) {
                if (processType == null || processType.equals(hpi.getProcessDefinitionKey())) {
                    result.add(getWorkflowHistory(hpi.getId()));
                }
            }
        }

        return result;
    }

    @Transactional
    public void handoffTask(String taskId, String toUserId, String reason, String fromUserId) {
        Task task = taskService.createTaskQuery()
                .taskId(taskId)
                .singleResult();

        if (task == null) {
            throw new RuntimeException("Task not found: " + taskId);
        }

        Map<String, Object> variables = runtimeService.getVariables(task.getProcessInstanceId());
        List<Map<String, Object>> handoffHistory = getHandoffHistoryList(variables);

        Map<String, Object> handoffRecord = new HashMap<>();
        handoffRecord.put("id", UUID.randomUUID().toString());
        handoffRecord.put("taskId", taskId);
        handoffRecord.put("taskName", task.getName());
        handoffRecord.put("fromUserId", fromUserId);
        handoffRecord.put("toUserId", toUserId);
        handoffRecord.put("reason", reason);
        handoffRecord.put("timestamp", LocalDateTime.now().toString());
        handoffHistory.add(handoffRecord);

        runtimeService.setVariable(task.getProcessInstanceId(), "handoffHistory", serializeList(handoffHistory));

        // Unclaim and reassign
        if (task.getAssignee() != null) {
            taskService.unclaim(taskId);
        }
        taskService.claim(taskId, toUserId);

        log.info("Task {} handed off from {} to {} - Reason: {}", taskId, fromUserId, toUserId, reason);
    }

    @Transactional
    public ApprovalDTO recordApproval(String taskId, String decision, String comments, String userId) {
        Task task = taskService.createTaskQuery()
                .taskId(taskId)
                .singleResult();

        if (task == null) {
            throw new RuntimeException("Task not found: " + taskId);
        }

        Map<String, Object> variables = runtimeService.getVariables(task.getProcessInstanceId());
        String currentLevel = (String) variables.getOrDefault("currentLevel", "SUPERVISOR");

        List<Map<String, Object>> approvalHistory = getApprovalHistoryList(variables);

        Map<String, Object> approvalRecord = new HashMap<>();
        approvalRecord.put("id", UUID.randomUUID().toString());
        approvalRecord.put("taskId", taskId);
        approvalRecord.put("taskName", task.getName());
        approvalRecord.put("approverId", userId);
        approvalRecord.put("approverLevel", currentLevel);
        approvalRecord.put("decision", decision);
        approvalRecord.put("comments", comments);
        approvalRecord.put("timestamp", LocalDateTime.now().toString());
        approvalRecord.put("stepOrder", approvalHistory.size() + 1);
        approvalHistory.add(approvalRecord);

        // Update variables
        Map<String, Object> updateVars = new HashMap<>();
        updateVars.put("approvalHistory", serializeList(approvalHistory));
        updateVars.put("decision", decision.toLowerCase());
        updateVars.put("approvalComments", comments);
        updateVars.put("completedBy", userId);

        taskService.complete(taskId, updateVars);

        log.info("Approval recorded for task {} by {} - Decision: {}", taskId, userId, decision);

        return ApprovalDTO.builder()
                .id((String) approvalRecord.get("id"))
                .processInstanceId(task.getProcessInstanceId())
                .taskId(taskId)
                .taskName(task.getName())
                .approverId(userId)
                .approverLevel(currentLevel)
                .decision(decision)
                .comments(comments)
                .timestamp(LocalDateTime.now())
                .stepOrder(approvalHistory.size())
                .isRequired(true)
                .build();
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
    private List<Map<String, Object>> getHandoffHistoryList(Map<String, Object> variables) {
        Object historyObj = variables.get("handoffHistory");
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

    private String serializeList(List<Map<String, Object>> list) {
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }
}
