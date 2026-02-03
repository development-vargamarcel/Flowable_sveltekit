package com.demo.bpm.service;

import com.demo.bpm.exception.InvalidOperationException;
import com.demo.bpm.exception.ResourceNotFoundException;
import com.demo.bpm.repository.ProcessConfigRepository;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProcessServiceTest {

    @Mock
    private RuntimeService runtimeService;
    @Mock
    private RepositoryService repositoryService;
    @Mock
    private HistoryService historyService;
    @Mock
    private ProcessConfigRepository processConfigRepository;
    @Mock
    private BusinessTableService businessTableService;

    @Mock
    private ProcessInstanceQuery processInstanceQuery;

    @InjectMocks
    private ProcessService processService;

    @Test
    void cancelProcessInstance_Success_Initiator() {
        // Setup
        String processInstanceId = "proc1";
        String userId = "user1";
        String reason = "Mistake";

        ProcessInstance processInstance = mock(ProcessInstance.class);
        when(processInstance.getId()).thenReturn(processInstanceId);

        when(runtimeService.createProcessInstanceQuery()).thenReturn(processInstanceQuery);
        when(processInstanceQuery.processInstanceId(processInstanceId)).thenReturn(processInstanceQuery);
        when(processInstanceQuery.singleResult()).thenReturn(processInstance);

        when(runtimeService.getVariable(processInstanceId, "_startedBy")).thenReturn(userId);

        // Execute
        processService.cancelProcessInstance(processInstanceId, reason, userId, false);

        // Verify
        verify(runtimeService).deleteProcessInstance(processInstanceId, reason);
    }

    @Test
    void cancelProcessInstance_Success_Admin() {
        // Setup
        String processInstanceId = "proc1";
        String userId = "admin"; // Admin user
        String starterId = "user1"; // Started by someone else
        String reason = "Admin override";

        ProcessInstance processInstance = mock(ProcessInstance.class);
        when(processInstance.getId()).thenReturn(processInstanceId);

        when(runtimeService.createProcessInstanceQuery()).thenReturn(processInstanceQuery);
        when(processInstanceQuery.processInstanceId(processInstanceId)).thenReturn(processInstanceQuery);
        when(processInstanceQuery.singleResult()).thenReturn(processInstance);

        when(runtimeService.getVariable(processInstanceId, "_startedBy")).thenReturn(starterId);

        // Execute
        processService.cancelProcessInstance(processInstanceId, reason, userId, true);

        // Verify
        verify(runtimeService).deleteProcessInstance(processInstanceId, reason);
    }

    @Test
    void cancelProcessInstance_Fail_Unauthorized() {
        // Setup
        String processInstanceId = "proc1";
        String userId = "otherUser";
        String starterId = "user1";

        ProcessInstance processInstance = mock(ProcessInstance.class);
        when(processInstance.getId()).thenReturn(processInstanceId);

        when(runtimeService.createProcessInstanceQuery()).thenReturn(processInstanceQuery);
        when(processInstanceQuery.processInstanceId(processInstanceId)).thenReturn(processInstanceQuery);
        when(processInstanceQuery.singleResult()).thenReturn(processInstance);

        when(runtimeService.getVariable(processInstanceId, "_startedBy")).thenReturn(starterId);

        // Execute & Verify
        assertThrows(InvalidOperationException.class, () ->
            processService.cancelProcessInstance(processInstanceId, "Reason", userId, false)
        );
        verify(runtimeService, never()).deleteProcessInstance(anyString(), anyString());
    }

    @Test
    void cancelProcessInstance_Fail_NotFound() {
        // Setup
        String processInstanceId = "proc1";
        String userId = "user1";

        when(runtimeService.createProcessInstanceQuery()).thenReturn(processInstanceQuery);
        when(processInstanceQuery.processInstanceId(processInstanceId)).thenReturn(processInstanceQuery);
        when(processInstanceQuery.singleResult()).thenReturn(null); // Not found

        // Execute & Verify
        assertThrows(ResourceNotFoundException.class, () ->
            processService.cancelProcessInstance(processInstanceId, "Reason", userId, false)
        );
        verify(runtimeService, never()).deleteProcessInstance(anyString(), anyString());
    }
}
