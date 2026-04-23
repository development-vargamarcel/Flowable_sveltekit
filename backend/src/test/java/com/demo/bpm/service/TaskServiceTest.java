package com.demo.bpm.service;

import com.demo.bpm.dto.TaskDTO;
import com.demo.bpm.exception.InvalidOperationException;
import com.demo.bpm.exception.ResourceNotFoundException;
import com.demo.bpm.repository.ProcessConfigRepository;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.repository.ProcessDefinitionQuery;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceQuery;
import org.flowable.task.api.Task;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyMap;
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
    private com.demo.bpm.mapper.TaskMapper taskMapper;
    @Mock
    private com.demo.bpm.service.helpers.TaskCommonHelper taskCommonHelper;
    @Mock
    private com.demo.bpm.service.helpers.VariableHelper variableHelper;

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

    @Test
    void completeTask_Success() {
        String taskId = "task1";
        String userId = "user1";
        Map<String, Object> variables = Map.of("days", 5);

        Task task = mock(Task.class);
        when(task.getId()).thenReturn(taskId);
        when(task.getAssignee()).thenReturn(userId);
        when(task.getProcessInstanceId()).thenReturn("pi1");
        when(task.getProcessDefinitionId()).thenReturn("pd1");

        when(taskCommonHelper.getTaskOrThrow(taskId)).thenReturn(task);

        ProcessInstanceQuery piQuery = mock(ProcessInstanceQuery.class);
        when(runtimeService.createProcessInstanceQuery()).thenReturn(piQuery);
        when(piQuery.processInstanceId("pi1")).thenReturn(piQuery);
        when(piQuery.singleResult()).thenReturn(mock(ProcessInstance.class));

        ProcessDefinitionQuery pdQuery = mock(ProcessDefinitionQuery.class);
        when(repositoryService.createProcessDefinitionQuery()).thenReturn(pdQuery);
        when(pdQuery.processDefinitionId("pd1")).thenReturn(pdQuery);
        when(pdQuery.singleResult()).thenReturn(mock(ProcessDefinition.class));

        taskService.completeTask(taskId, variables, userId);

        verify(flowableTaskService).complete(eq(taskId), anyMap());
    }

    @Test
    void completeTask_TaskNotFound_ThrowsException() {
        String taskId = "invalid";
        when(taskCommonHelper.getTaskOrThrow(taskId)).thenThrow(ResourceNotFoundException.class);

        assertThrows(ResourceNotFoundException.class, () ->
            taskService.completeTask(taskId, Collections.emptyMap(), "user1"));
    }

    @Test
    void completeTask_AssignedToOther_ThrowsException() {
        String taskId = "task1";
        Task task = mock(Task.class);
        when(task.getAssignee()).thenReturn("otherUser");

        when(taskCommonHelper.getTaskOrThrow(taskId)).thenReturn(task);
        doThrow(InvalidOperationException.class).when(taskCommonHelper).validateAssigneeOrUnassigned(task, "user1");

        assertThrows(InvalidOperationException.class, () ->
            taskService.completeTask(taskId, Collections.emptyMap(), "user1"));
    }

    @Test
    void claimTask_Success() {
        String taskId = "task1";
        String userId = "user1";
        Task task = mock(Task.class);
        when(task.getAssignee()).thenReturn(null);

        when(taskCommonHelper.getTaskOrThrow(taskId)).thenReturn(task);

        taskService.claimTask(taskId, userId);

        verify(flowableTaskService).claim(taskId, userId);
    }

    @Test
    void claimTask_AlreadyAssigned_ThrowsException() {
        String taskId = "task1";
        Task task = mock(Task.class);
        when(task.getAssignee()).thenReturn("otherUser");

        when(taskCommonHelper.getTaskOrThrow(taskId)).thenReturn(task);

        assertThrows(InvalidOperationException.class, () ->
            taskService.claimTask(taskId, "user1"));
    }

    @Test
    void delegateTask_Success() {
        String taskId = "task1";
        String currentUserId = "user1";
        String targetUserId = "user2";
        Task task = mock(Task.class);
        when(task.getAssignee()).thenReturn(currentUserId);

        when(taskCommonHelper.getTaskOrThrow(taskId)).thenReturn(task);

        taskService.delegateTask(taskId, currentUserId, targetUserId);

        verify(flowableTaskService).setAssignee(taskId, targetUserId);
    }

    @Test
    void unclaimTask_Success() {
        String taskId = "task1";
        Task task = mock(Task.class);

        when(taskCommonHelper.getTaskOrThrow(taskId)).thenReturn(task);

        taskService.unclaimTask(taskId);

        verify(flowableTaskService).unclaim(taskId);
    }
}
