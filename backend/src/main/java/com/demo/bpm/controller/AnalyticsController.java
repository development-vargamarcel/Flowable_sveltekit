package com.demo.bpm.controller;

import com.demo.bpm.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/process-duration")
    public ResponseEntity<List<AnalyticsService.DurationBin>> getProcessDuration(
            @RequestParam(required = false) String processDefinitionKey) {
        return ResponseEntity.ok(analyticsService.getProcessDurationDistribution(processDefinitionKey));
    }

    @GetMapping("/user-performance")
    public ResponseEntity<List<AnalyticsService.UserPerformanceMetric>> getUserPerformance() {
        return ResponseEntity.ok(analyticsService.getUserPerformanceStats());
    }

    @GetMapping("/bottlenecks")
    public ResponseEntity<List<AnalyticsService.BottleneckMetric>> getBottlenecks() {
        return ResponseEntity.ok(analyticsService.getBottlenecks());
    }

    @GetMapping("/completion-trend")
    public ResponseEntity<List<AnalyticsService.TrendMetric>> getProcessCompletionTrend(
            @RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(analyticsService.getProcessCompletionTrend(days));
    }
}
