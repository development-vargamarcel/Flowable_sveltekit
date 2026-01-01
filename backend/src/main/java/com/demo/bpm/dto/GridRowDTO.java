package com.demo.bpm.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GridRowDTO {

    private Long id;
    private Long documentId;
    private String processInstanceId;
    private String gridName;
    private Integer rowIndex;

    // Field values mapped by field name
    @Builder.Default
    private Map<String, Object> fields = new HashMap<>();
}
