package com.demo.bpm.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class ProcessDTO {
    private String key;
    private String name;
    private String description;
    private Integer version;
}
