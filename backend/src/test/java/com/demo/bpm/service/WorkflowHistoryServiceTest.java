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
import org.flowable.variable.api.history.HistoricVariableInstanceQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.flowable.task.api.TaskQuery;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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
    private com.demo.bpm.mapper.WorkflowHistoryMapper workflowHistoryMapper;
    @Mock
    private com.demo.bpm.service.helpers.VariableHelper variableHelper;

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
        when(variableHelper.getMergedVariables("pi1")).thenReturn(Map.of("startedBy", "user1"));

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

    @Test
    void getWorkflowHistory_ActiveInstance() {
        String procId = "pi1";
        ProcessInstance pi = mock(ProcessInstance.class);
        when(pi.getId()).thenReturn(procId);
        when(pi.getProcessDefinitionId()).thenReturn("pd1");
        when(pi.getStartTime()).thenReturn(new Date());
        when(pi.isSuspended()).thenReturn(false);

        when(runtimeService.createProcessInstanceQuery()).thenReturn(processInstanceQuery);
        when(processInstanceQuery.processInstanceId(procId)).thenReturn(processInstanceQuery);
        when(processInstanceQuery.singleResult()).thenReturn(pi);

        when(variableHelper.getMergedVariables(procId)).thenReturn(Map.of("startedBy", "user1", "currentLevel", "SUPERVISOR"));

        ProcessDefinition pd = mock(ProcessDefinition.class);
        when(pd.getName()).thenReturn("My Process");
        when(repositoryService.createProcessDefinitionQuery()).thenReturn(processDefinitionQuery);
        when(processDefinitionQuery.processDefinitionId("pd1")).thenReturn(processDefinitionQuery);
        when(processDefinitionQuery.singleResult()).thenReturn(pd);

        when(taskService.createTaskQuery()).thenReturn(taskQuery);
        when(taskQuery.processInstanceId(procId)).thenReturn(taskQuery);
        when(taskQuery.list()).thenReturn(List.of());

        when(historyService.createHistoricTaskInstanceQuery()).thenReturn(historicTaskInstanceQuery);
        when(historicTaskInstanceQuery.processInstanceId(procId)).thenReturn(historicTaskInstanceQuery);
        when(historicTaskInstanceQuery.orderByHistoricTaskInstanceEndTime()).thenReturn(historicTaskInstanceQuery);
        when(historicTaskInstanceQuery.asc()).thenReturn(historicTaskInstanceQuery);
        when(historicTaskInstanceQuery.list()).thenReturn(List.of());

        WorkflowHistoryDTO history = workflowHistoryService.getWorkflowHistory(procId);

        assertNotNull(history);
        assertEquals(procId, history.getProcessInstanceId());
        assertEquals("ACTIVE", history.getStatus());
        assertEquals("My Process", history.getProcessDefinitionName());
    }

    @Test
    void getWorkflowHistory_CompletedInstance() {
        String procId = "pi1";
        when(runtimeService.createProcessInstanceQuery()).thenReturn(processInstanceQuery);
        when(processInstanceQuery.processInstanceId(procId)).thenReturn(processInstanceQuery);
        when(processInstanceQuery.singleResult()).thenReturn(null);

        HistoricProcessInstance hpi = mock(HistoricProcessInstance.class);
        when(hpi.getId()).thenReturn(procId);
        when(hpi.getProcessDefinitionId()).thenReturn("pd1");
        when(hpi.getStartTime()).thenReturn(new Date());
        when(hpi.getEndTime()).thenReturn(new Date());
        when(hpi.getDurationInMillis()).thenReturn(5000L);

        when(historyService.createHistoricProcessInstanceQuery()).thenReturn(historicProcessInstanceQuery);
        when(historicProcessInstanceQuery.processInstanceId(procId)).thenReturn(historicProcessInstanceQuery);
        when(historicProcessInstanceQuery.singleResult()).thenReturn(hpi);

        when(variableHelper.getMergedVariables(procId)).thenReturn(Map.of());

        when(taskService.createTaskQuery()).thenReturn(taskQuery);
        when(taskQuery.processInstanceId(procId)).thenReturn(taskQuery);
        when(taskQuery.list()).thenReturn(List.of());

        when(historyService.createHistoricTaskInstanceQuery()).thenReturn(historicTaskInstanceQuery);
        when(historicTaskInstanceQuery.processInstanceId(procId)).thenReturn(historicTaskInstanceQuery);
        when(historicTaskInstanceQuery.orderByHistoricTaskInstanceEndTime()).thenReturn(historicTaskInstanceQuery);
        when(historicTaskInstanceQuery.asc()).thenReturn(historicTaskInstanceQuery);
        when(historicTaskInstanceQuery.list()).thenReturn(List.of());

        WorkflowHistoryDTO history = workflowHistoryService.getWorkflowHistory(procId);

        assertNotNull(history);
        assertEquals("COMPLETED", history.getStatus());
        assertEquals(5000L, history.getDurationInMillis());
    }
}
