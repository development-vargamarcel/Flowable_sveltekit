package com.demo.bpm.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessConfigDTO {

    private Long id;
    private String processDefinitionKey;
    private Boolean persistOnTaskComplete;
    private Boolean persistOnProcessComplete;
}
