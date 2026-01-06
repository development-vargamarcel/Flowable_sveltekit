package com.demo.bpm.dto.schema;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GridColumnDTO {
    private String id;
    @NotBlank
    private String name;
    @NotBlank
    private String label;
    @NotBlank
    private String type;
    private Boolean required;
    private String placeholder;
    private List<Map<String, String>> options;
    private FormFieldValidationDTO validation;
    private String hiddenExpression;
    private String readonlyExpression;
    private String requiredExpression;
    private String calculationExpression;
}
