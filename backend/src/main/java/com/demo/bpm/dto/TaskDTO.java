package com.demo.bpm.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class TaskDTO {
    private String id;
    private String name;
    private String description;
    private String processInstanceId;
    private String processDefinitionKey;
    private String processName;
    private String assignee;
    private String owner;
    private LocalDateTime createTime;
    private LocalDateTime dueDate;
    private Integer priority;
    private String formKey;
    private Map<String, Object> variables;
    private String businessKey;
}
