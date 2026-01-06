package com.demo.bpm.integration;

import com.demo.bpm.dto.ProcessDTO;
import com.demo.bpm.exception.ResourceNotFoundException;
import com.demo.bpm.service.ProcessService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProcessServiceIT extends BaseIntegrationTest {

    @Autowired
    private ProcessService processService;

    @Test
    void getProcessById_whenProcessExists_shouldReturnProcess() {
        String processDefinitionId = processService.getAvailableProcesses().stream()
                .filter(p -> p.getKey().equals("leave-request"))
                .findFirst()
                .map(ProcessDTO::getId)
                .orElseThrow(() -> new IllegalStateException("Process 'leave-request' not found for testing"));

        ProcessDTO process = processService.getProcessById(processDefinitionId);

        assertThat(process).isNotNull();
        assertThat(process.getId()).isEqualTo(processDefinitionId);
        assertThat(process.getKey()).isEqualTo("leave-request");
    }

    @Test
    void getProcessById_whenProcessDoesNotExist_shouldThrowException() {
        assertThatThrownBy(() -> processService.getProcessById("nonexistent"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Process not found with id: nonexistent");
    }
}
