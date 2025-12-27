package com.demo.bpm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardDTO {
    // Summary Statistics
    private DashboardStats stats;

    // Active Processes by Type
    private Map<String, Long> activeByType;

    // Processes by Status
    private Map<String, Long> byStatus;

    // Recent Activity
    private List<WorkflowHistoryDTO> recentCompleted;
    private List<WorkflowHistoryDTO> activeProcesses;
    private List<WorkflowHistoryDTO> myPendingApprovals;

    // Escalation Metrics
    private EscalationMetrics escalationMetrics;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DashboardStats {
        private long totalActive;
        private long totalCompleted;
        private long totalPending;
        private long myTasks;
        private long myProcesses;
        private long pendingEscalations;
        private long avgCompletionTimeHours;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EscalationMetrics {
        private long totalEscalations;
        private long totalDeEscalations;
        private long activeEscalatedProcesses;
        private Map<String, Long> escalationsByLevel;
    }
}
