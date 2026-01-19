package com.demo.bpm.service;

import com.demo.bpm.dto.DashboardDTO;
import com.demo.bpm.dto.WorkflowHistoryDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final RuntimeService runtimeService;
    private final HistoryService historyService;
    private final TaskService taskService;
    private final WorkflowHistoryService workflowHistoryService;

    public DashboardDTO getDashboard(String userId, Pageable pageable, String status, String type) {
        // Parallelize or optimize these counts if database load is high, but for now sequential is okay.
        long totalActive = runtimeService.createProcessInstanceQuery().count();
        long totalCompleted = historyService.createHistoricProcessInstanceQuery().finished().count();
        long totalPending = taskService.createTaskQuery().count();
        long myTasks = taskService.createTaskQuery().taskCandidateOrAssigned(userId).count();
        long myProcesses = runtimeService.createProcessInstanceQuery().variableValueEquals("startedBy", userId).count();
        long pendingEscalations = runtimeService.createProcessInstanceQuery().variableValueGreaterThan("escalationCount", 0).count();

        // Optimize: Fetch IDs first or fetch lighter objects if possible.
        // For dashboard lists, we might not need full history details for every item in the list immediately
        // but the current DTO structure requires WorkflowHistoryDTO.

        List<ProcessInstance> activeProcessesForDisplay = runtimeService.createProcessInstanceQuery()
                .orderByStartTime().desc()
                .listPage((int) pageable.getOffset(), pageable.getPageSize());

        List<HistoricProcessInstance> completedProcesses = historyService.createHistoricProcessInstanceQuery()
                .finished()
                .orderByProcessInstanceEndTime().desc()
                .listPage((int) pageable.getOffset(), pageable.getPageSize());

        long avgCompletionTimeHours = calculateAvgCompletionTime(completedProcesses);

        // Active distribution
        Map<String, Long> activeByType = getActiveByTypeDistribution();

        Map<String, Long> byStatus = new HashMap<>();
        byStatus.put("ACTIVE", totalActive);
        byStatus.put("COMPLETED", totalCompleted);
        byStatus.put("PENDING", totalPending);

        // Convert to DTOs.
        // Note: workflowHistoryService.getWorkflowHistory might be expensive (N+1).
        // If performance becomes an issue, we should batch fetch variables/history.
        // For now, let's keep it but be aware.

        List<WorkflowHistoryDTO> recentCompletedList = completedProcesses.stream()
                .map(hp -> workflowHistoryService.getWorkflowHistory(hp.getId()))
                .collect(Collectors.toList());
        Page<WorkflowHistoryDTO> recentCompleted = new PageImpl<>(recentCompletedList, pageable, totalCompleted);

        List<WorkflowHistoryDTO> activeWithDetailsList = activeProcessesForDisplay.stream()
                .map(ap -> workflowHistoryService.getWorkflowHistory(ap.getId()))
                .collect(Collectors.toList());
        Page<WorkflowHistoryDTO> activeWithDetails = new PageImpl<>(activeWithDetailsList, pageable, totalActive);

        List<Task> userTasks = taskService.createTaskQuery()
                .taskCandidateOrAssigned(userId)
                .orderByTaskCreateTime().desc()
                .listPage((int) pageable.getOffset(), pageable.getPageSize());

        List<WorkflowHistoryDTO> myPendingApprovalsList = userTasks.stream()
                .map(t -> workflowHistoryService.getWorkflowHistory(t.getProcessInstanceId()))
                .distinct()
                .collect(Collectors.toList());
        Page<WorkflowHistoryDTO> myPendingApprovals = new PageImpl<>(myPendingApprovalsList, pageable, myTasks);

        DashboardDTO.EscalationMetrics escalationMetrics = getEscalationMetrics(pendingEscalations);

        DashboardDTO.DashboardStats stats = DashboardDTO.DashboardStats.builder()
                .totalActive(totalActive)
                .totalCompleted(totalCompleted)
                .totalPending(totalPending)
                .myTasks(myTasks)
                .myProcesses(myProcesses)
                .pendingEscalations(pendingEscalations)
                .avgCompletionTimeHours(avgCompletionTimeHours)
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

    private long calculateAvgCompletionTime(List<HistoricProcessInstance> completedProcesses) {
        if (completedProcesses.isEmpty()) return 0;

        long totalMillis = completedProcesses.stream()
                .filter(p -> p.getDurationInMillis() != null)
                .mapToLong(HistoricProcessInstance::getDurationInMillis)
                .sum();

        return totalMillis / (completedProcesses.size() * 3600000);
    }

    private Map<String, Long> getActiveByTypeDistribution() {
        // Use a native query or better API if available to avoid fetching entities.
        // Flowable doesn't have a direct "group by" query API for runtime processes easily without native queries.
        // We limit to 100 to avoid memory issues.
        List<ProcessInstance> activeProcessesForGrouping = runtimeService.createProcessInstanceQuery()
                .orderByStartTime().desc()
                .listPage(0, 100);
        return activeProcessesForGrouping.stream()
                .collect(Collectors.groupingBy(ProcessInstance::getProcessDefinitionKey, Collectors.counting()));
    }

    private DashboardDTO.EscalationMetrics getEscalationMetrics(long pendingEscalations) {
        // Optimized to avoid N+1 for getting currentLevel.
        // We can include process variables in the query.

        List<ProcessInstance> escalatedSample = runtimeService.createProcessInstanceQuery()
                .variableValueGreaterThan("escalationCount", 0)
                .includeProcessVariables() // This fetches variables in a single query (or fewer queries)
                .orderByStartTime().desc()
                .listPage(0, 50);

        Map<String, Long> escalationsByLevel = new HashMap<>();

        for (ProcessInstance pi : escalatedSample) {
            Map<String, Object> vars = pi.getProcessVariables();
            if (vars != null && vars.containsKey("currentLevel")) {
                String currentLevel = vars.get("currentLevel").toString();
                escalationsByLevel.merge(currentLevel, 1L, Long::sum);
            }
        }

        return DashboardDTO.EscalationMetrics.builder()
                .totalEscalations(pendingEscalations) // Approximation
                .totalDeEscalations(0) // Not easily trackable without history query
                .activeEscalatedProcesses(pendingEscalations)
                .escalationsByLevel(escalationsByLevel)
                .build();
    }
}
