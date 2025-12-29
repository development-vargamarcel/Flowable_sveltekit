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
public class FormFieldDTO {
    private String id;
    private String name;
    private String label;
    private String type;
    private boolean required;
    private Map<String, Object> validation;
    private List<Map<String, String>> options;
    private String placeholder;
    private String defaultValue;
    private String defaultExpression;
    private String tooltip;
    private boolean readonly;
    private boolean hidden;
    private String hiddenExpression;
    private String readonlyExpression;
    private String requiredExpression;
    private int gridColumn;
    private int gridRow;
    private int gridWidth;
    private String cssClass;
    private String onChange;
    private String onBlur;
}
