package com.demo.bpm.service;

import com.demo.bpm.dto.DashboardDTO;
import com.demo.bpm.dto.WorkflowHistoryDTO;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceQuery;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private RuntimeService runtimeService;
    @Mock
    private HistoryService historyService;
    @Mock
    private TaskService taskService;
    @Mock
    private WorkflowHistoryService workflowHistoryService;

    @Mock
    private ProcessInstanceQuery processInstanceQuery;
    @Mock
    private HistoricProcessInstanceQuery historicProcessInstanceQuery;
    @Mock
    private TaskQuery taskQuery;

    @InjectMocks
    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        // Common mocks
        lenient().when(runtimeService.createProcessInstanceQuery()).thenReturn(processInstanceQuery);
        lenient().when(historyService.createHistoricProcessInstanceQuery()).thenReturn(historicProcessInstanceQuery);
        lenient().when(taskService.createTaskQuery()).thenReturn(taskQuery);
    }

    @Test
    void getDashboard_shouldReturnDashboardData() {
        // Counts
        when(processInstanceQuery.count()).thenReturn(10L);
        when(historicProcessInstanceQuery.finished()).thenReturn(historicProcessInstanceQuery);
        when(historicProcessInstanceQuery.count()).thenReturn(5L);
        when(taskQuery.count()).thenReturn(3L);
        when(taskQuery.taskCandidateOrAssigned(anyString())).thenReturn(taskQuery);

        // Need separate mock calls for different query constraints if they return the same mock object
        // Mockito handles this by chaining, but here I'm reusing the same mock object 'processInstanceQuery'
        // which might be problematic if 'variableValueEquals' is called multiple times.
        // For simplicity, we assume the builder methods return 'this' (the mock).
        when(processInstanceQuery.variableValueEquals(anyString(), anyString())).thenReturn(processInstanceQuery);
        when(processInstanceQuery.variableValueGreaterThan(anyString(), anyInt())).thenReturn(processInstanceQuery);

        // Active processes list
        when(processInstanceQuery.orderByStartTime()).thenReturn(processInstanceQuery);
        when(processInstanceQuery.desc()).thenReturn(processInstanceQuery);
        ProcessInstance processInstance = mock(ProcessInstance.class);
        when(processInstance.getId()).thenReturn("proc1");
        when(processInstance.getProcessDefinitionKey()).thenReturn("myProcess");
        when(processInstanceQuery.listPage(anyInt(), anyInt())).thenReturn(List.of(processInstance));

        // Completed processes list
        when(historicProcessInstanceQuery.orderByProcessInstanceEndTime()).thenReturn(historicProcessInstanceQuery);
        when(historicProcessInstanceQuery.desc()).thenReturn(historicProcessInstanceQuery);
        HistoricProcessInstance historicProcessInstance = mock(HistoricProcessInstance.class);
        when(historicProcessInstance.getId()).thenReturn("hist1");
        when(historicProcessInstance.getDurationInMillis()).thenReturn(3600000L);
        when(historicProcessInstanceQuery.listPage(anyInt(), anyInt())).thenReturn(List.of(historicProcessInstance));

        // User tasks
        when(taskQuery.orderByTaskCreateTime()).thenReturn(taskQuery);
        when(taskQuery.desc()).thenReturn(taskQuery);
        Task task = mock(Task.class);
        when(task.getProcessInstanceId()).thenReturn("proc1");
        when(taskQuery.listPage(anyInt(), anyInt())).thenReturn(List.of(task));

        // WorkflowHistoryService
        WorkflowHistoryDTO historyDTO = WorkflowHistoryDTO.builder().processInstanceId("proc1").build();
        when(workflowHistoryService.getWorkflowHistory(anyString())).thenReturn(historyDTO);

        Pageable pageable = PageRequest.of(0, 10);
        DashboardDTO dashboard = dashboardService.getDashboard("user1", pageable, null, null);

        assertNotNull(dashboard);
        assertEquals(10L, dashboard.getStats().getTotalActive());
        assertEquals(5L, dashboard.getStats().getTotalCompleted());
        assertEquals(3L, dashboard.getStats().getTotalPending());
        assertEquals(1L, dashboard.getStats().getAvgCompletionTimeHours());
    }
}
