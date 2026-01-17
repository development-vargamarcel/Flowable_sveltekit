package com.demo.bpm.controller;

import com.demo.bpm.dto.SlaStatsDTO;
import com.demo.bpm.service.SLAService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SLAController.class)
class SLAControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SLAService slaService;

    // Mocking UserService as it's often required by Security configuration
    @MockBean
    private com.demo.bpm.service.UserService userService;

    @Test
    @WithMockUser
    void getStats_shouldReturnSlaStats() throws Exception {
        SlaStatsDTO stats = SlaStatsDTO.builder()
                .totalProcesses(10)
                .onTrack(5)
                .atRisk(3)
                .breached(2)
                .avgCompletionPercentage(45.5)
                .processesByStatus(Collections.emptyList())
                .build();

        when(slaService.getSLAStats()).thenReturn(stats);

        mockMvc.perform(get("/api/slas/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProcesses").value(10))
                .andExpect(jsonPath("$.onTrack").value(5))
                .andExpect(jsonPath("$.atRisk").value(3))
                .andExpect(jsonPath("$.breached").value(2))
                .andExpect(jsonPath("$.avgCompletionPercentage").value(45.5));
    }
}
