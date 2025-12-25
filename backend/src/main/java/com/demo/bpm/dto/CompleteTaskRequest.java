package com.demo.bpm.dto;

import lombok.Data;

import java.util.Map;

@Data
public class CompleteTaskRequest {
    private Map<String, Object> variables;
}
