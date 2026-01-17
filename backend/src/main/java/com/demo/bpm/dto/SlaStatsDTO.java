package com.demo.bpm.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class SlaStatsDTO {
    private long totalProcesses;
    private long onTrack;
    private long atRisk;
    private long breached;
    private double avgCompletionPercentage;
    private List<StatusCount> processesByStatus;

    @Data
    @Builder
    public static class StatusCount {
        private String status;
        private long count;
    }
}
