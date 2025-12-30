package com.demo.bpm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for complete process-level form configuration.
 * Contains field library, global condition rules, and grid configuration.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessFormConfigDTO {
    private String processDefinitionId;
    private ProcessFieldLibraryDTO fieldLibrary;
    private List<FieldConditionRuleDTO> globalConditions;
    private GridConfigDTO defaultGridConfig;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GridConfigDTO {
        private int columns;
        private int gap;
    }
}
