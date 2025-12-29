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
public class FormGridDTO {
    private String id;
    private String name;
    private String label;
    private String description;
    private int minRows;
    private int maxRows;
    private List<GridColumnDTO> columns;
    private int gridColumn;
    private int gridRow;
    private int gridWidth;
    private String cssClass;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GridColumnDTO {
        private String id;
        private String name;
        private String label;
        private String type;
        private boolean required;
        private String placeholder;
        private List<String> options;
        private Double min;
        private Double max;
        private Double step;
        private Map<String, Object> validation;
    }
}
