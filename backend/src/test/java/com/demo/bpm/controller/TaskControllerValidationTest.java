package com.demo.bpm.controller;

import com.demo.bpm.dto.ProcessFormConfigDTO;
import com.demo.bpm.dto.FormDefinitionDTO;
import com.demo.bpm.exception.GlobalExceptionHandler;
import com.demo.bpm.service.FormDefinitionService;
import com.demo.bpm.service.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class TaskControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TaskService taskService;

    @MockBean
    private FormDefinitionService formDefinitionService;

    @Test
    @WithMockUser(username = "alice")
    void completeTask_withNullVariablesFailsValidation() throws Exception {
        mockMvc.perform(post("/api/tasks/123/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"variables\":null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Error"))
                .andExpect(jsonPath("$.fieldErrors.variables").value("Variables map cannot be null"))
                .andExpect(jsonPath("$.path").value("/api/tasks/123/complete"));
    }

    @Test
    @WithMockUser(username = "alice")
    void delegateTask_missingTargetUserIdReturns400() throws Exception {
        mockMvc.perform(post("/api/tasks/123/delegate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("targetUserId is required"));
    }
}
