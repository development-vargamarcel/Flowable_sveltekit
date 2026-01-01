package com.demo.bpm.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentDTO {

    private Long id;
    private String processInstanceId;
    private String businessKey;
    private String processDefinitionKey;
    private String processDefinitionName;

    // Field values mapped by field name
    @Builder.Default
    private Map<String, Object> fields = new HashMap<>();

    // Grid data mapped by grid name
    @Builder.Default
    private Map<String, List<Map<String, Object>>> grids = new HashMap<>();

    // Audit info
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}
