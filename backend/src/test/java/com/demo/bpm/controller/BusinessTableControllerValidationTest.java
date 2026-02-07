package com.demo.bpm.controller;

import com.demo.bpm.exception.GlobalExceptionHandler;
import com.demo.bpm.service.BusinessTableService;
import com.demo.bpm.service.ProcessService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BusinessTableController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class BusinessTableControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BusinessTableService businessTableService;

    @MockBean
    private ProcessService processService;

    @Test
    @WithMockUser
    void saveDraft_requiresProcessDefinitionKey() throws Exception {
        mockMvc.perform(post("/api/business/save-draft")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Error"))
                .andExpect(jsonPath("$.fieldErrors.processDefinitionKey").value("processDefinitionKey is required"))
                .andExpect(jsonPath("$.path").value("/api/business/save-draft"));
    }

    @Test
    @WithMockUser
    void saveGridRows_requiresProcessDefinitionKeyAndRows() throws Exception {
        mockMvc.perform(post("/api/business/document-types/123/grids/main")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.processDefinitionKey").value("processDefinitionKey is required"))
                .andExpect(jsonPath("$.fieldErrors.rows").value("rows are required"))
                .andExpect(jsonPath("$.path").value("/api/business/document-types/123/grids/main"));
    }
}
