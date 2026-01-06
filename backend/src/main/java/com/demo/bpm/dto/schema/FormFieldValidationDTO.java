package com.demo.bpm.dto.schema;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FormFieldValidationDTO {
    private Integer minLength;
    private Integer maxLength;
    private Double min;
    private Double max;
    private String pattern;
    private String patternMessage;
    private String message;
    private String customExpression;
    private String customMessage;
    private List<String> allowedMimeTypes;
    private Long maxFileSize;
}
