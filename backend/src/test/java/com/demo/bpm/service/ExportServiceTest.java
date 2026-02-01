package com.demo.bpm.service;

import com.demo.bpm.dto.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExportServiceTest {

    @Mock
    private WorkflowHistoryService workflowHistoryService;

    @InjectMocks
    private ExportService exportService;

    @Test
    void exportProcessInstance_shouldReturnCsvBytes() {
        // Setup
        String processInstanceId = "123";
        LocalDateTime now = LocalDateTime.now();

        WorkflowHistoryDTO history = WorkflowHistoryDTO.builder()
                .processInstanceId(processInstanceId)
                .processDefinitionName("Test Process")
                .processDefinitionKey("test-process")
                .businessKey("BK-123")
                .status("ACTIVE")
                .initiatorId("user1")
                .initiatorName("User One")
                .startTime(now)
                .endTime(null)
                .currentLevel("SUPERVISOR")
                .variables(Map.of("amount", 100))
                .taskHistory(List.of(
                        TaskHistoryDTO.builder()
                                .name("Task 1")
                                .assignee("user2")
                                .createTime(now)
                                .build()
                ))
                .comments(List.of(
                        CommentDTO.builder()
                                .authorId("user1")
                                .message("Hello")
                                .timestamp(now)
                                .build()
                ))
                .build();

        when(workflowHistoryService.getWorkflowHistory(processInstanceId)).thenReturn(history);

        // Execute
        byte[] result = exportService.exportProcessInstance(processInstanceId);

        // Verify
        assertNotNull(result);
        String csv = new String(result);
        assertTrue(csv.contains("PROCESS DETAILS"));
        assertTrue(csv.contains("Test Process"));
        assertTrue(csv.contains("VARIABLES"));
        assertTrue(csv.contains("amount"));
        assertTrue(csv.contains("100"));
        assertTrue(csv.contains("TASK HISTORY"));
        assertTrue(csv.contains("Task 1"));
        assertTrue(csv.contains("COMMENTS"));
        assertTrue(csv.contains("Hello"));
    }
}
