package com.demo.bpm.service;

import com.demo.bpm.dto.EscalationDTO;
import com.demo.bpm.dto.EscalationRequest;
import com.demo.bpm.exception.InvalidOperationException;
import com.demo.bpm.service.helpers.HistoryRecorder;
import com.demo.bpm.util.WorkflowConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WorkflowServiceTest {

    @Mock
    private RuntimeService runtimeService;
    @Mock
    private TaskService taskService;
    @Mock
    private HistoryRecorder historyRecorder;

    // ObjectMapper is likely no longer used directly in WorkflowService or it's used via HistoryRecorder/Utils
    // but check if it's injected. The WorkflowService now injects HistoryRecorder.

    @Mock
    private TaskQuery taskQuery;

    @InjectMocks
    private WorkflowService workflowService;

    @BeforeEach
    void setUp() {
        when(taskService.createTaskQuery()).thenReturn(taskQuery);
    }

    @Test
    void escalateTask_shouldEscalateSuccessfully() throws Exception {
        // Setup
        String taskId = "task1";
        EscalationRequest request = new EscalationRequest();
        request.setReason("High value");
        request.setTargetLevel("MANAGER");
        String userId = "supervisor1";

        Task task = mock(Task.class);
        when(task.getProcessInstanceId()).thenReturn("proc1");

        when(taskQuery.taskId(taskId)).thenReturn(taskQuery);
        when(taskQuery.singleResult()).thenReturn(task);

        when(runtimeService.getVariables("proc1")).thenReturn(Map.of(WorkflowConstants.VAR_CURRENT_LEVEL, "SUPERVISOR", WorkflowConstants.VAR_ESCALATION_COUNT, 0));

        when(historyRecorder.recordEscalationHistory(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyBoolean(), anyMap()))
            .thenReturn("historyId");

        // Execute
        EscalationDTO result = workflowService.escalateTask(taskId, request, userId);

        // Verify
        assertNotNull(result);
        assertEquals("MANAGER", result.getToLevel());
        // verify(taskService).complete(eq(taskId), any(Map.class)); // WorkflowService changed to not pass map to complete for escalation
        verify(taskService).complete(eq(taskId));
        verify(runtimeService).setVariables(eq("proc1"), any(Map.class));
    }

    @Test
    void deEscalateTask_shouldDeEscalateSuccessfully() throws Exception {
        // Setup
        String taskId = "task1";
        EscalationRequest request = new EscalationRequest();
        request.setReason("Correction");
        request.setTargetLevel("SUPERVISOR");
        String userId = "manager1";

        Task task = mock(Task.class);
        when(task.getProcessInstanceId()).thenReturn("proc1");

        when(taskQuery.taskId(taskId)).thenReturn(taskQuery);
        when(taskQuery.singleResult()).thenReturn(task);

        when(runtimeService.getVariables("proc1")).thenReturn(Map.of(WorkflowConstants.VAR_CURRENT_LEVEL, "MANAGER"));

        when(historyRecorder.recordEscalationHistory(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyBoolean(), anyMap()))
            .thenReturn("historyId");

        // Execute
        EscalationDTO result = workflowService.deEscalateTask(taskId, request, userId);

        // Verify
        assertNotNull(result);
        assertEquals("SUPERVISOR", result.getToLevel());
        verify(taskService).complete(eq(taskId));
        verify(runtimeService).setVariables(eq("proc1"), any(Map.class));
    }

    @Test
    void escalateTask_whenInvalidLevel_shouldThrowException() {
         // Setup
        String taskId = "task1";
        EscalationRequest request = new EscalationRequest();
        request.setTargetLevel("INVALID_LEVEL");
        String userId = "supervisor1";

        Task task = mock(Task.class);
        when(task.getProcessInstanceId()).thenReturn("proc1");

        when(taskQuery.taskId(taskId)).thenReturn(taskQuery);
        when(taskQuery.singleResult()).thenReturn(task);

        when(runtimeService.getVariables("proc1")).thenReturn(Map.of(WorkflowConstants.VAR_CURRENT_LEVEL, "SUPERVISOR"));

        // Execute & Verify
        assertThrows(InvalidOperationException.class, () -> workflowService.escalateTask(taskId, request, userId));
    }

    @Test
    void escalateTask_whenAlreadyAtTopLevel_shouldThrowException() {
        // Setup
        String taskId = "task1";
        EscalationRequest request = new EscalationRequest();
        request.setReason("Test");
        String userId = "executive1";

        Task task = mock(Task.class);
        when(task.getProcessInstanceId()).thenReturn("proc1");

        when(taskQuery.taskId(taskId)).thenReturn(taskQuery);
        when(taskQuery.singleResult()).thenReturn(task);

        when(runtimeService.getVariables("proc1")).thenReturn(Map.of(WorkflowConstants.VAR_CURRENT_LEVEL, "EXECUTIVE"));

        // Execute & Verify
        assertThrows(InvalidOperationException.class, () -> workflowService.escalateTask(taskId, request, userId));
    }
}
