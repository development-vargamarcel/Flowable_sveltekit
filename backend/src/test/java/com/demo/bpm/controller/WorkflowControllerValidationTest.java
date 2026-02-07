package com.demo.bpm.controller;

import com.demo.bpm.dto.EscalationRequest;
import com.demo.bpm.exception.GlobalExceptionHandler;
import com.demo.bpm.service.DashboardService;
import com.demo.bpm.service.WorkflowHistoryService;
import com.demo.bpm.service.WorkflowService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WorkflowController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class WorkflowControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WorkflowService workflowService;

    @MockBean
    private WorkflowHistoryService workflowHistoryService;

    @MockBean
    private DashboardService dashboardService;

    @Test
    @WithMockUser
    void addComment_requiresMessage() throws Exception {
        mockMvc.perform(post("/api/workflow/processes/123/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.message").value("Comment message is required"))
                .andExpect(jsonPath("$.path").value("/api/workflow/processes/123/comments"));
    }

    @Test
    @WithMockUser
    void handoff_requiresTargetUser() throws Exception {
        mockMvc.perform(post("/api/workflow/tasks/123/handoff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.toUserId").value("Target user is required"))
                .andExpect(jsonPath("$.path").value("/api/workflow/tasks/123/handoff"));
    }

    @Test
    @WithMockUser
    void approve_requiresComments() throws Exception {
        mockMvc.perform(post("/api/workflow/tasks/123/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.comments").value("Comments are required"))
                .andExpect(jsonPath("$.path").value("/api/workflow/tasks/123/approve"));
    }

    @Test
    @WithMockUser
    void escalate_usesValidatedRequest() throws Exception {
        mockMvc.perform(post("/api/workflow/tasks/123/escalate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.reason").value("Reason is required"))
                .andExpect(jsonPath("$.path").value("/api/workflow/tasks/123/escalate"));

        verify(workflowService, org.mockito.Mockito.never()).escalateTask(any(), any(EscalationRequest.class), any());
    }
}
