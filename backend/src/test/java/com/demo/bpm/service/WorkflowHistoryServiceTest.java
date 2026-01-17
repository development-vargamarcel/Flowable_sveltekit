package com.demo.bpm.service;

import com.demo.bpm.dto.WorkflowHistoryDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.repository.ProcessDefinitionQuery;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.flowable.task.api.TaskQuery;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkflowHistoryServiceTest {

    @Mock
    private RuntimeService runtimeService;
    @Mock
    private HistoryService historyService;
    @Mock
    private TaskService taskService;
    @Mock
    private RepositoryService repositoryService;
    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ProcessInstanceQuery processInstanceQuery;
    @Mock
    private HistoricProcessInstanceQuery historicProcessInstanceQuery;
    @Mock
    private TaskQuery taskQuery;
    @Mock
    private ProcessDefinitionQuery processDefinitionQuery;
    @Mock
    private org.flowable.task.api.history.HistoricTaskInstanceQuery historicTaskInstanceQuery;

    @InjectMocks
    private WorkflowHistoryService workflowHistoryService;

    @BeforeEach
    void setUp() {
        lenient().when(runtimeService.createProcessInstanceQuery()).thenReturn(processInstanceQuery);
        lenient().when(historyService.createHistoricProcessInstanceQuery()).thenReturn(historicProcessInstanceQuery);
        lenient().when(taskService.createTaskQuery()).thenReturn(taskQuery);
        lenient().when(repositoryService.createProcessDefinitionQuery()).thenReturn(processDefinitionQuery);
        lenient().when(historyService.createHistoricTaskInstanceQuery()).thenReturn(historicTaskInstanceQuery);
    }

    @Test
    void getAllProcesses_whenFilteringByProcessType_shouldApplyFilter() {
        // Setup
        when(processInstanceQuery.orderByStartTime()).thenReturn(processInstanceQuery);
        when(processInstanceQuery.desc()).thenReturn(processInstanceQuery);

        // This is the key verification: processDefinitionKey MUST be called
        when(processInstanceQuery.processDefinitionKey("myProcess")).thenReturn(processInstanceQuery);

        ProcessInstance pi = mock(ProcessInstance.class);
        when(pi.getId()).thenReturn("pi1");
        when(pi.getProcessDefinitionId()).thenReturn("pd1");
        when(pi.getStartTime()).thenReturn(new Date());

        when(processInstanceQuery.listPage(0, 10)).thenReturn(List.of(pi));

        // Mock getWorkflowHistory internals
        when(runtimeService.createProcessInstanceQuery().processInstanceId("pi1")).thenReturn(processInstanceQuery);
        when(processInstanceQuery.singleResult()).thenReturn(pi);
        when(runtimeService.getVariables("pi1")).thenReturn(Map.of("startedBy", "user1"));

        when(taskQuery.processInstanceId("pi1")).thenReturn(taskQuery);

        when(historicTaskInstanceQuery.processInstanceId("pi1")).thenReturn(historicTaskInstanceQuery);
        when(historicTaskInstanceQuery.orderByHistoricTaskInstanceEndTime()).thenReturn(historicTaskInstanceQuery);
        when(historicTaskInstanceQuery.asc()).thenReturn(historicTaskInstanceQuery);
        when(historicTaskInstanceQuery.list()).thenReturn(List.of());

        ProcessDefinition pd = mock(ProcessDefinition.class);
        when(processDefinitionQuery.processDefinitionId("pd1")).thenReturn(processDefinitionQuery);
        when(processDefinitionQuery.singleResult()).thenReturn(pd);

        // Execute
        List<WorkflowHistoryDTO> results = workflowHistoryService.getAllProcesses("ACTIVE", "myProcess", 0, 10);

        // Verify
        verify(processInstanceQuery).processDefinitionKey("myProcess");
        assertEquals(1, results.size());
    }
}
