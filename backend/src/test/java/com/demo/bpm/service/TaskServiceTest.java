package com.demo.bpm.service;

import com.demo.bpm.dto.TaskDTO;
import com.demo.bpm.repository.ProcessConfigRepository;
import com.demo.bpm.service.helpers.TaskQueryHelper;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.task.api.TaskQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TaskServiceTest {

    @Mock
    private org.flowable.engine.TaskService flowableTaskService;
    @Mock
    private RuntimeService runtimeService;
    @Mock
    private RepositoryService repositoryService;
    @Mock
    private HistoryService historyService;
    @Mock
    private BusinessTableService businessTableService;
    @Mock
    private ProcessConfigRepository processConfigRepository;
    @Mock
    private TaskQueryHelper taskQueryHelper;

    @Mock
    private TaskQuery taskQuery;

    @InjectMocks
    private TaskService taskService;

    @Test
    void getGroupTasks_NoFilters() {
        // Setup
        String userId = "user1";

        when(flowableTaskService.createTaskQuery()).thenReturn(taskQuery);
        when(taskQuery.taskCandidateOrAssigned(userId)).thenReturn(taskQuery);
        when(taskQuery.orderByTaskPriority()).thenReturn(taskQuery);
        when(taskQuery.desc()).thenReturn(taskQuery);
        when(taskQuery.orderByTaskCreateTime()).thenReturn(taskQuery);
        when(taskQuery.list()).thenReturn(Collections.emptyList());

        // Execute
        List<TaskDTO> result = taskService.getGroupTasks(userId, null, null, null);

        // Verify
        verify(taskQuery).taskCandidateOrAssigned(userId);
        verify(taskQuery, never()).taskNameLikeIgnoreCase(anyString());
        verify(taskQuery, never()).taskPriority(anyInt());
        verify(taskQuery, never()).taskAssignee(anyString());
    }

    @Test
    void getGroupTasks_AllFilters() {
        // Setup
        String userId = "user1";
        String text = "test";
        String assignee = "user1";
        Integer priority = 50;

        when(flowableTaskService.createTaskQuery()).thenReturn(taskQuery);
        when(taskQuery.taskCandidateOrAssigned(userId)).thenReturn(taskQuery);
        when(taskQuery.taskNameLikeIgnoreCase(anyString())).thenReturn(taskQuery);
        when(taskQuery.taskPriority(anyInt())).thenReturn(taskQuery);
        when(taskQuery.taskAssignee(anyString())).thenReturn(taskQuery);

        when(taskQuery.orderByTaskPriority()).thenReturn(taskQuery);
        when(taskQuery.desc()).thenReturn(taskQuery);
        when(taskQuery.orderByTaskCreateTime()).thenReturn(taskQuery);
        when(taskQuery.list()).thenReturn(Collections.emptyList());

        // Execute
        taskService.getGroupTasks(userId, text, assignee, priority);

        // Verify
        verify(taskQuery).taskCandidateOrAssigned(userId);
        verify(taskQuery).taskNameLikeIgnoreCase("%test%");
        verify(taskQuery).taskPriority(50);
        verify(taskQuery).taskAssignee(assignee);
    }

    @Test
    void getGroupTasks_UnassignedFilter() {
        // Setup
        String userId = "user1";
        String assignee = "unassigned";

        when(flowableTaskService.createTaskQuery()).thenReturn(taskQuery);
        when(taskQuery.taskCandidateOrAssigned(userId)).thenReturn(taskQuery);
        when(taskQuery.taskUnassigned()).thenReturn(taskQuery);

        when(taskQuery.orderByTaskPriority()).thenReturn(taskQuery);
        when(taskQuery.desc()).thenReturn(taskQuery);
        when(taskQuery.orderByTaskCreateTime()).thenReturn(taskQuery);
        when(taskQuery.list()).thenReturn(Collections.emptyList());

        // Execute
        taskService.getGroupTasks(userId, null, assignee, null);

        // Verify
        verify(taskQuery).taskCandidateOrAssigned(userId);
        verify(taskQuery).taskUnassigned();
        verify(taskQuery, never()).taskAssignee(anyString());
    }
}
