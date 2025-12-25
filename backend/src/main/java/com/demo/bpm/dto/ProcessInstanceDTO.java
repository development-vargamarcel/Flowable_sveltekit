package com.demo.bpm.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class ProcessInstanceDTO {
    private String id;
    private String processDefinitionKey;
    private String processDefinitionName;
    private String businessKey;
    private LocalDateTime startTime;
    private String startUserId;
    private Map<String, Object> variables;
    private boolean ended;
}
