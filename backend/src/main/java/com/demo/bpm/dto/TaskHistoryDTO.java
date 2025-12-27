package com.demo.bpm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskHistoryDTO {
    private String id;
    private String taskDefinitionKey;
    private String name;
    private String description;
    private String processInstanceId;
    private String assignee;
    private String owner;
    private LocalDateTime createTime;
    private LocalDateTime claimTime;
    private LocalDateTime endTime;
    private Long durationInMillis;
    private String deleteReason; // Completed, Escalated, De-escalated, etc.
    private Map<String, Object> variables;
}
