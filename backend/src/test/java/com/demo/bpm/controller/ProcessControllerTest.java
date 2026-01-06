package com.demo.bpm.controller;

import com.demo.bpm.dto.ProcessDTO;
import com.demo.bpm.exception.ResourceNotFoundException;
import com.demo.bpm.service.ProcessService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProcessController.class)
class ProcessControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProcessService processService;

    @MockBean
    private com.demo.bpm.service.UserService userService;

    @MockBean
    private com.demo.bpm.service.FormDefinitionService formDefinitionService;

    @Test
    @WithMockUser
    void getProcessById_whenProcessExists_shouldReturnProcess() throws Exception {
        ProcessDTO process = ProcessDTO.builder()
                .id("testprocess:1:123")
                .key("testprocess")
                .name("Test Process")
                .version(1)
                .build();

        when(processService.getProcessById("testprocess:1:123")).thenReturn(process);

        mockMvc.perform(get("/api/processes/testprocess:1:123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("testprocess:1:123"))
                .andExpect(jsonPath("$.key").value("testprocess"))
                .andExpect(jsonPath("$.name").value("Test Process"))
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    @WithMockUser
    void getProcessById_whenProcessDoesNotExist_shouldReturnNotFound() throws Exception {
        when(processService.getProcessById("nonexistent")).thenThrow(new ResourceNotFoundException("Process not found"));

        mockMvc.perform(get("/api/processes/nonexistent"))
                .andExpect(status().isNotFound());
    }
}
