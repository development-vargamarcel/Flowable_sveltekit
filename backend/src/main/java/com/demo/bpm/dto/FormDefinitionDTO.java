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
public class FormDefinitionDTO {
    private String elementId;
    private String elementName;
    private String elementType;
    private List<FormFieldDTO> fields;
    private List<FormGridDTO> grids;
    private GridConfigDTO gridConfig;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GridConfigDTO {
        private int columns;
        private int gap;
    }
}
