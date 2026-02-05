package com.demo.bpm.service;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.HistoryService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private final HistoryService historyService;

    @Data
    @Builder
    public static class DurationBin {
        private String label;
        private long count;
        private long minDurationMs;
        private long maxDurationMs;
    }

    @Data
    @Builder
    public static class UserPerformanceMetric {
        private String userId;
        private long tasksCompleted;
        private double avgDurationHours;
    }

    @Data
    @Builder
    public static class BottleneckMetric {
        private String processDefinitionKey;
        private String taskName;
        private String taskDefinitionKey;
        private double avgDurationHours;
        private long slowInstanceCount; // Count of instances taking > 2x average
        private long totalInstances;
    }

    @Data
    @Builder
    public static class TrendMetric {
        private String date;
        private long count;
    }

    /**
     * Get process duration distribution for finished processes
     */
    public List<DurationBin> getProcessDurationDistribution(String processDefinitionKey) {
        var query = historyService.createHistoricProcessInstanceQuery()
                .finished()
                .orderByProcessInstanceEndTime().desc();
        
        if (processDefinitionKey != null && !processDefinitionKey.isEmpty()) {
            query.processDefinitionKey(processDefinitionKey);
        }

        // Limit to last 1000 instances for performance in this demo
        List<HistoricProcessInstance> instances = query.listPage(0, 1000);

        // Define Bins: <1h, 1h-4h, 4h-24h, 1d-3d, >3d
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("< 1h", 0L);
        counts.put("1h - 4h", 0L);
        counts.put("4h - 24h", 0L);
        counts.put("1d - 3d", 0L);
        counts.put("> 3d", 0L);

        for (HistoricProcessInstance instance : instances) {
            Long duration = instance.getDurationInMillis();
            if (duration == null) continue;

            double hours = duration / (1000.0 * 60 * 60);

            if (hours < 1) counts.put("< 1h", counts.get("< 1h") + 1);
            else if (hours < 4) counts.put("1h - 4h", counts.get("1h - 4h") + 1);
            else if (hours < 24) counts.put("4h - 24h", counts.get("4h - 24h") + 1);
            else if (hours < 72) counts.put("1d - 3d", counts.get("1d - 3d") + 1);
            else counts.put("> 3d", counts.get("> 3d") + 1);
        }

        return counts.entrySet().stream()
                .map(e -> DurationBin.builder()
                        .label(e.getKey())
                        .count(e.getValue())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Get user performance metrics (completed tasks)
     */
    public List<UserPerformanceMetric> getUserPerformanceStats() {
        // Fetch last 1000 completed tasks
        List<HistoricTaskInstance> tasks = historyService.createHistoricTaskInstanceQuery()
                .finished()
                .taskAssigneeLike("%") // Only tasks with assignees
                .orderByHistoricTaskInstanceEndTime().desc()
                .listPage(0, 1000);

        Map<String, List<Long>> userDurations = new HashMap<>();

        for (HistoricTaskInstance task : tasks) {
            String assignee = task.getAssignee();
            if (assignee == null) continue;

            Long duration = task.getDurationInMillis();
            if (duration == null) continue;

            userDurations.computeIfAbsent(assignee, k -> new ArrayList<>()).add(duration);
        }

        return userDurations.entrySet().stream()
                .map(entry -> {
                    String user = entry.getKey();
                    List<Long> durations = entry.getValue();
                    double avgMs = durations.stream().mapToLong(Long::longValue).average().orElse(0);
                    return UserPerformanceMetric.builder()
                            .userId(user)
                            .tasksCompleted(durations.size())
                            .avgDurationHours(avgMs / (1000.0 * 60 * 60))
                            .build();
                })
                .sorted(Comparator.comparing(UserPerformanceMetric::getTasksCompleted).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Identify potential bottlenecks (slow tasks)
     */
    public List<BottleneckMetric> getBottlenecks() {
        // Fetch last 2000 finished tasks
        List<HistoricTaskInstance> tasks = historyService.createHistoricTaskInstanceQuery()
                .finished()
                .orderByHistoricTaskInstanceEndTime().desc()
                .listPage(0, 2000);

        // Group by ProcessKey + TaskDefKey
        Map<String, List<Long>> taskDurations = new HashMap<>();
        Map<String, String> taskNames = new HashMap<>(); // Key -> Human name

        for (HistoricTaskInstance task : tasks) {
            if (task.getDurationInMillis() == null) continue;
            
            String key = task.getProcessDefinitionId().split(":")[0] + "::" + task.getTaskDefinitionKey();
            taskDurations.computeIfAbsent(key, k -> new ArrayList<>()).add(task.getDurationInMillis());
            taskNames.putIfAbsent(key, task.getName());
        }

        List<BottleneckMetric> bottlenecks = new ArrayList<>();

        for (Map.Entry<String, List<Long>> entry : taskDurations.entrySet()) {
            String key = entry.getKey();
            List<Long> durations = entry.getValue();
            
            double avgMs = durations.stream().mapToLong(v -> v).average().orElse(0);
            
            // Count "slow" instances (e.g., > 1.5x average)
            long slowCount = durations.stream()
                    .filter(d -> d > (avgMs * 1.5))
                    .count();

            // Only report if we have enough data and significant slowness
            if (durations.size() > 5) { // Needs minimum sample size
                String[] parts = key.split("::");
                bottlenecks.add(BottleneckMetric.builder()
                        .processDefinitionKey(parts[0])
                        .taskDefinitionKey(parts[1])
                        .taskName(taskNames.get(key))
                        .avgDurationHours(avgMs / (1000.0 * 60 * 60))
                        .totalInstances(durations.size())
                        .slowInstanceCount(slowCount)
                        .build());
            }
        }
        
        // Sort by Average Duration Descending
        bottlenecks.sort(Comparator.comparing(BottleneckMetric::getAvgDurationHours).reversed());
        return bottlenecks;
    }

    /**
     * Get process completion trend for the last N days
     */
    public List<TrendMetric> getProcessCompletionTrend(int days) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -days);
        Date startDate = cal.getTime();

        List<HistoricProcessInstance> instances = historyService.createHistoricProcessInstanceQuery()
                .finished()
                .finishedAfter(startDate)
                .orderByProcessInstanceEndTime().asc()
                .list();

        Map<String, Long> dailyCounts = new TreeMap<>();
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");

        // Initialize all days with 0 to ensure continuity
        Calendar c = Calendar.getInstance();
        for (int i = 0; i < days; i++) {
            dailyCounts.put(sdf.format(c.getTime()), 0L);
            c.add(Calendar.DAY_OF_YEAR, -1);
        }

        for (HistoricProcessInstance instance : instances) {
            if (instance.getEndTime() != null) {
                String dateStr = sdf.format(instance.getEndTime());
                // Only count if it falls within our range (safe guard)
                if (dailyCounts.containsKey(dateStr)) {
                    dailyCounts.put(dateStr, dailyCounts.get(dateStr) + 1);
                }
            }
        }

        return dailyCounts.entrySet().stream()
                .map(e -> TrendMetric.builder()
                        .date(e.getKey())
                        .count(e.getValue())
                        .build())
                .sorted(Comparator.comparing(TrendMetric::getDate))
                .collect(Collectors.toList());
    }
}
