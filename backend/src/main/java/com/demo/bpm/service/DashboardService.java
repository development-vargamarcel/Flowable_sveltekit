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
        long avgCompletionTimeHours = calculateAvgCompletionTime(completedProcesses);

        // For activeByType, query a smaller sample for representative distribution
        Map<String, Long> activeByType = getActiveByTypeDistribution();

        // Group by status
        Map<String, Long> byStatus = new HashMap<>();
        byStatus.put("ACTIVE", totalActive);
        byStatus.put("COMPLETED", totalCompleted);
        byStatus.put("PENDING", totalPending);

        // Get recent completed DTOs
        Page<WorkflowHistoryDTO> recentCompleted = new PageImpl<>(completedProcesses.stream()
                .map(hp -> workflowHistoryService.getWorkflowHistory(hp.getId()))
                .collect(Collectors.toList()), pageable, totalCompleted);

        // Get active with details from the paginated list
        Page<WorkflowHistoryDTO> activeWithDetails = new PageImpl<>(activeProcessesForDisplay.stream()
                .map(ap -> workflowHistoryService.getWorkflowHistory(ap.getId()))
                .collect(Collectors.toList()), pageable, totalActive);

        // Get user's pending approvals with pagination
        List<Task> userTasks = taskService.createTaskQuery()
                .taskCandidateOrAssigned(userId)
                .orderByTaskCreateTime().desc()
                .listPage((int) pageable.getOffset(), pageable.getPageSize());

        Page<WorkflowHistoryDTO> myPendingApprovals = new PageImpl<>(userTasks.stream()
                .map(t -> workflowHistoryService.getWorkflowHistory(t.getProcessInstanceId()))
                .distinct()
                .collect(Collectors.toList()), pageable, myTasks);

        // Escalation metrics
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
        return avgCompletionTimeHours;
    }

    private Map<String, Long> getActiveByTypeDistribution() {
        // 100 is enough to get process type distribution without performance impact
        List<ProcessInstance> activeProcessesForGrouping = runtimeService.createProcessInstanceQuery()
                .orderByStartTime().desc()
                .listPage(0, 100);
        return activeProcessesForGrouping.stream()
                .collect(Collectors.groupingBy(ProcessInstance::getProcessDefinitionKey, Collectors.counting()));
    }

    private DashboardDTO.EscalationMetrics getEscalationMetrics(long pendingEscalations) {
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

        return DashboardDTO.EscalationMetrics.builder()
                .totalEscalations(totalEscalations)
                .totalDeEscalations(totalDeEscalations)
                .activeEscalatedProcesses(activeEscalatedProcesses)
                .escalationsByLevel(escalationsByLevel)
                .build();
    }
}
