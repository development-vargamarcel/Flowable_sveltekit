package com.demo.bpm.dto.schema;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FormGridDTO {
    private String id;
    @NotBlank
    private String name;
    @NotBlank
    private String label;
    private String description;
    private Integer minRows;
    private Integer maxRows;
    @Valid
    private List<GridColumnDTO> columns;
    private Integer gridColumn;
    private Integer gridRow;
    private Integer gridWidth;
    private String cssClass;
    private String visibilityExpression;
    private Boolean enablePagination;
    private Integer pageSize;
    private Boolean enableSorting;
    private Boolean enableRowActions;
    private Boolean enableImportExport;
    private Boolean enableGrouping;
    private String groupByColumn;
}
