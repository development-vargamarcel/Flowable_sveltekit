package com.demo.bpm.controller;

import com.demo.bpm.dto.ProcessDTO;
import com.demo.bpm.dto.ProcessInstanceDTO;
import com.demo.bpm.dto.UserDTO;
import com.demo.bpm.exception.ResourceNotFoundException;
import com.demo.bpm.service.ProcessService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProcessController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(com.demo.bpm.exception.GlobalExceptionHandler.class)
class ProcessControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProcessService processService;

    @MockBean
    private com.demo.bpm.service.UserService userService;

    @MockBean
    private com.demo.bpm.service.FormDefinitionService formDefinitionService;

    @MockBean
    private com.demo.bpm.service.ExportService exportService;

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

    @Test
    @WithMockUser(username = "jane")
    void startProcess_handlesMissingRequestBody() throws Exception {
        when(userService.getUserInfo(any())).thenReturn(UserDTO.builder().displayName("Jane Doe").build());
        when(processService.startProcess(eq("demo"), eq(null), any(Map.class), eq("jane")))
                .thenReturn(ProcessInstanceDTO.builder().id("123").build());

        mockMvc.perform(post("/api/processes/demo/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processInstance.id").value("123"));

        verify(processService, times(1))
                .startProcess(eq("demo"), eq(null), any(Map.class), eq("jane"));
    }

    @Test
    @WithMockUser(username = "jane")
    void startProcess_handlesNullVariablesInRequest() throws Exception {
        when(userService.getUserInfo(any())).thenReturn(UserDTO.builder().displayName("Jane Doe").build());
        when(processService.startProcess(eq("demo"), eq("BK-1"), any(Map.class), eq("jane")))
                .thenReturn(ProcessInstanceDTO.builder().id("234").build());

        mockMvc.perform(post("/api/processes/demo/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"businessKey\":\"BK-1\",\"variables\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processInstance.id").value("234"));

        verify(processService, times(1))
                .startProcess(eq("demo"), eq("BK-1"), any(Map.class), eq("jane"));
    }

    @Test
    @WithMockUser
    void deployProcess_requiresNameAndXml() throws Exception {
        mockMvc.perform(post("/api/processes/deploy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.processName").value("processName is required"))
                .andExpect(jsonPath("$.fieldErrors.bpmnXml").value("bpmnXml is required"))
                .andExpect(jsonPath("$.path").value("/api/processes/deploy"));
    }

    @Test
    @WithMockUser
    void updateCategory_requiresCategory() throws Exception {
        mockMvc.perform(put("/api/processes/def-1/category")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.category").value("category is required"))
                .andExpect(jsonPath("$.path").value("/api/processes/def-1/category"));
    }
}
