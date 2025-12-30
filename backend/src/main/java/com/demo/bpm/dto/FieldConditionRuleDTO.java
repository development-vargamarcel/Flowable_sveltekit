package com.demo.bpm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * DTO for field condition rules that control visibility and editability.
 * Supports "least access wins" logic where the most restrictive rule takes effect.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldConditionRuleDTO {
    private String id;
    private String name;
    private String description;
    private String condition;           // Expression: "${amount > 1000}"
    private String effect;              // hidden, visible, readonly, editable
    private ConditionTargetDTO target;  // What this rule affects
    private int priority;               // Higher = evaluated first
    private boolean enabled;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConditionTargetDTO {
        private String type;            // all, field, grid, column
        private List<String> fieldNames;
        private List<String> gridNames;
        private List<ColumnTargetDTO> columnTargets;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ColumnTargetDTO {
        private String gridName;
        private List<String> columnNames;
    }
}
