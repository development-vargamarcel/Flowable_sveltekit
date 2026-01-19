package com.demo.bpm.service.helpers;

import com.demo.bpm.util.WorkflowConstants;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.flowable.engine.RuntimeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HistoryRecorderTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private RuntimeService runtimeService;

    private HistoryRecorder historyRecorder;

    @BeforeEach
    void setUp() {
        historyRecorder = new HistoryRecorder(objectMapper, runtimeService);
    }

    @Test
    void recordEscalationHistory_ShouldAddRecordAndSetVariable() throws JsonProcessingException {
        // Arrange
        String processInstanceId = "proc-1";
        String taskId = "task-1";
        String userId = "user-1";
        String currentLevel = "SUPERVISOR";
        String targetLevel = "MANAGER";
        String reason = "Too complex";
        boolean isEscalation = true;
        Map<String, Object> variables = new HashMap<>();

        // Mock ObjectMapper behavior
        when(objectMapper.convertValue(any(), eq(List.class))).thenReturn(new ArrayList<>());
        when(objectMapper.writeValueAsString(any())).thenReturn("[]");

        // Act
        String resultId = historyRecorder.recordEscalationHistory(
            processInstanceId, taskId, userId, currentLevel, targetLevel,
            reason, isEscalation, variables
        );

        // Assert
        assertNotNull(resultId);
        verify(runtimeService).setVariable(eq(processInstanceId), eq(WorkflowConstants.VAR_ESCALATION_HISTORY), anyString());
    }

    @Test
    void recordHandoffHistory_ShouldAddRecordAndSetVariable() throws JsonProcessingException {
        // Arrange
        String processInstanceId = "proc-1";
        String taskId = "task-1";
        String taskName = "Task 1";
        String fromUserId = "user-1";
        String toUserId = "user-2";
        String reason = "Vacation";
        Map<String, Object> variables = new HashMap<>();

        // Mock ObjectMapper behavior
        when(objectMapper.convertValue(any(), eq(List.class))).thenReturn(new ArrayList<>());
        when(objectMapper.writeValueAsString(any())).thenReturn("[]");

        // Act
        historyRecorder.recordHandoffHistory(
            processInstanceId, taskId, taskName, fromUserId, toUserId, reason, variables
        );

        // Assert
        verify(runtimeService).setVariable(eq(processInstanceId), eq(WorkflowConstants.VAR_HANDOFF_HISTORY), anyString());
    }

    @Test
    void recordApprovalHistory_ShouldAddRecordAndSetVariable() throws JsonProcessingException {
        // Arrange
        String processInstanceId = "proc-1";
        String taskId = "task-1";
        String taskName = "Task 1";
        String userId = "user-1";
        String currentLevel = "SUPERVISOR";
        String decision = "APPROVE";
        String comments = "Looks good";
        Map<String, Object> variables = new HashMap<>();

        // Mock ObjectMapper behavior
        when(objectMapper.convertValue(any(), eq(List.class))).thenReturn(new ArrayList<>());
        when(objectMapper.writeValueAsString(any())).thenReturn("[]");

        // Act
        HistoryRecorder.ApprovalRecordResult result = historyRecorder.recordApprovalHistory(
            processInstanceId, taskId, taskName, userId, currentLevel, decision, comments, variables
        );

        // Assert
        assertNotNull(result.id());
        assertEquals(1, result.stepOrder());
        verify(runtimeService).setVariable(eq(processInstanceId), eq(WorkflowConstants.VAR_APPROVAL_HISTORY), anyString());
    }
}
