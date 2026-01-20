package com.demo.bpm.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

@Data
public class CompleteTaskRequest {
    @NotNull(message = "Variables map cannot be null")
    private Map<String, Object> variables;
}
