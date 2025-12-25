package com.demo.bpm.dto;

import lombok.Data;

import java.util.Map;

@Data
public class StartProcessRequest {
    private String businessKey;
    private Map<String, Object> variables;
}
