package com.demo.bpm.service.helpers;

import com.demo.bpm.dto.DocumentDTO;
import com.demo.bpm.service.BusinessTableService;
import org.flowable.engine.RuntimeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VariableHelperTest {

    @Mock
    private RuntimeService runtimeService;

    @Mock
    private BusinessTableService businessTableService;

    @InjectMocks
    private VariableHelper variableHelper;

    @Test
    void getMergedVariables_shouldMergeSystemAndBusinessVariables() {
        String procId = "proc1";
        Map<String, Object> systemVars = Map.of("_startedBy", "user1");
        Map<String, Object> businessFields = Map.of("amount", 100.0);

        List<Map<String, Object>> rows = Collections.singletonList(Map.of("id", 1));
        Map<String, List<Map<String, Object>>> gridData = Map.of("items", rows);

        DocumentDTO doc = DocumentDTO.builder()
                .fields(businessFields)
                .grids(gridData)
                .build();

        when(runtimeService.getVariables(procId)).thenReturn(systemVars);
        when(businessTableService.getDocument(procId, "main")).thenReturn(Optional.of(doc));

        Map<String, Object> result = variableHelper.getMergedVariables(procId);

        assertEquals(3, result.size());
        assertEquals("user1", result.get("_startedBy"));
        assertEquals(100.0, result.get("amount"));
        assertEquals(rows, result.get("items"));
    }

    @Test
    void getMergedVariables_shouldHandleMissingFlowableVariables() {
        String procId = "proc1";
        when(runtimeService.getVariables(procId)).thenThrow(new RuntimeException("Ended"));
        when(businessTableService.getDocument(procId, "main")).thenReturn(Optional.empty());

        Map<String, Object> result = variableHelper.getMergedVariables(procId);

        assertTrue(result.isEmpty());
    }

    @Test
    void getMergedVariables_shouldHandleMissingBusinessDocument() {
        String procId = "proc1";
        Map<String, Object> systemVars = Map.of("_startedBy", "user1");

        when(runtimeService.getVariables(procId)).thenReturn(systemVars);
        when(businessTableService.getDocument(procId, "main")).thenReturn(Optional.empty());

        Map<String, Object> result = variableHelper.getMergedVariables(procId);

        assertEquals(1, result.size());
        assertEquals("user1", result.get("_startedBy"));
    }
}
