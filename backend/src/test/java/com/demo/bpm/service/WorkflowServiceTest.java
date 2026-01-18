package com.demo.bpm.service;

import com.demo.bpm.dto.EscalationDTO;
import com.demo.bpm.dto.EscalationRequest;
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

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkflowServiceTest {

    @Mock
    private RuntimeService runtimeService;
    @Mock
    private TaskService taskService;
    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private TaskQuery taskQuery;

    @InjectMocks
    private WorkflowService workflowService;

    @BeforeEach
    void setUp() {
        lenient().when(taskService.createTaskQuery()).thenReturn(taskQuery);
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

        when(runtimeService.getVariables("proc1")).thenReturn(Map.of("currentLevel", "SUPERVISOR", "escalationCount", 0));
        when(objectMapper.writeValueAsString(any())).thenReturn("[]");

        // Execute
        EscalationDTO result = workflowService.escalateTask(taskId, request, userId);

        // Verify
        assertNotNull(result);
        assertEquals("MANAGER", result.getToLevel());
        verify(taskService).complete(eq(taskId), any(Map.class));
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

        when(runtimeService.getVariables("proc1")).thenReturn(Map.of("currentLevel", "SUPERVISOR"));

        // Execute & Verify
        assertThrows(RuntimeException.class, () -> workflowService.escalateTask(taskId, request, userId));
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

        when(runtimeService.getVariables("proc1")).thenReturn(Map.of("currentLevel", "EXECUTIVE"));

        // Execute & Verify
        assertThrows(RuntimeException.class, () -> workflowService.escalateTask(taskId, request, userId));
    }
}
