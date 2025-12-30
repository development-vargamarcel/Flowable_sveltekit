package com.demo.bpm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for process-level field library.
 * Fields and grids are defined once per process and can be referenced by tasks.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessFieldLibraryDTO {
    private List<FormFieldDTO> fields;
    private List<FormGridDTO> grids;
}
