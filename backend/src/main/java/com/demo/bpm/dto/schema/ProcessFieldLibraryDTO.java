package com.demo.bpm.dto.schema;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProcessFieldLibraryDTO {
    @Valid
    private List<FormFieldDTO> fields;
    @Valid
    private List<FormGridDTO> grids;
}
