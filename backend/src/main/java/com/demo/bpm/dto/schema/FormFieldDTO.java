package com.demo.bpm.dto.schema;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FormFieldDTO {
    private String id;
    @NotBlank
    private String name;
    @NotBlank
    private String label;
    @NotBlank
    private String type;
    private Boolean required;
    private FormFieldValidationDTO validation;
    private List<Map<String, String>> options;
    private String placeholder;
    private String defaultValue;
    private String defaultExpression;
    private String tooltip;
    private Boolean readonly;
    private Boolean hidden;
    private Boolean richText;
    private Boolean signature;
    private String pickerType;
    private String hiddenExpression;
    private String readonlyExpression;
    private String requiredExpression;
    private String calculationExpression;
    private Integer gridColumn;
    private Integer gridRow;
    private Integer gridWidth;
    private String cssClass;
    private String onChange;
    private String onBlur;
}
